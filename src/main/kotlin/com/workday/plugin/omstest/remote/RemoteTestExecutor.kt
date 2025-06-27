package com.workday.plugin.omstest.remote

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.workday.plugin.omstest.junit.DummyRunConfiguration
import com.workday.plugin.omstest.junit.DummyTestLocator
import com.workday.plugin.omstest.parser.Status
import com.workday.plugin.omstest.parser.parseResultFile
import com.workday.plugin.omstest.util.LastTestStorage
import java.io.File
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object RemoteTestExecutor {

    fun runRemoteTestClass(
        project: Project,
        fqTestName: String,
        category: String,
        label: String
    ) {
        runRemoteTest(
            project,
            fqTestName,
            """empty $fqTestName empty empty $category /usr/local/workday-oms/logs/junit""",
            label
        )
    }

    fun runRemoteTestMethod(project: Project, fqTestName: String, label: String) {
        runRemoteTest(
            project,
            fqTestName,
            """$fqTestName empty empty empty OMSBI /usr/local/workday-oms/logs/junit""",
            label
        )
    }

    private fun runRemoteTest(
        project: Project,
        fqTestName: String,
        jmxParams: String,
        label: String
    ) {
        val dialog = HostPromptDialog()
        if (!dialog.showAndGet()) return
        val host = dialog.getHost()

        runTestWithHost(project, fqTestName, jmxParams, host, label)
    }

    fun runTestWithHost(
        project: Project,
        fqTestName: String,
        jmxParams: String,
        host: String,
        runTabName: String
    ) {
        LastTestStorage.setRemote(host, fqTestName, jmxParams, runTabName)

        val consoleView = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project).console

        val descriptor = RunContentDescriptor(
            consoleView,
            null,
            consoleView.component,
            "$runTabName on $host"
        )

        RunContentManager.getInstance(project)
            .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)

        val jmxInput = """
            open localhost:12016
            domain com.workday.oms
            bean name=JunitTestListener
            run executeTestSuite $jmxParams
        """.trimIndent().replace("\n", "\\n")

        val sshCommand = buildSshCommand(host, jmxInput)
        val scpCommand = buildScpCommand(project, host)

        val notification = notifyUser(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            runCommand(sshCommand, consoleView, "Running test on $host")
            runCommand(scpCommand, consoleView, "Fetching logs from $host")
            notification.expire()
            printTestResultLinks(project, consoleView)
            displayParsedResults(project)
        }
    }

    private fun displayParsedResults(project: Project) {
        val file = File(project.basePath, "TEST-junit-jupiter.xml")
        if (!file.exists()) return

        ApplicationManager.getApplication().invokeLater {
            val dummyConfig = DummyRunConfiguration(project)
            val consoleProperties = object : SMTRunnerConsoleProperties(
                dummyConfig,
                "ParsedResults",
                DefaultRunExecutor.getRunExecutorInstance()
            ) {
                override fun getTestLocator(): SMTestLocator = DummyTestLocator
            }

            val consoleView = SMTestRunnerConnectionUtil.createConsole("ParsedResults", consoleProperties)
            val processHandler = object : com.intellij.execution.process.ProcessHandler() {
                override fun destroyProcessImpl() = notifyProcessTerminated(0)
                override fun detachProcessImpl() = notifyProcessDetached()
                override fun detachIsDefault(): Boolean = false
                override fun getProcessInput(): OutputStream? = null
            }
            consoleView.attachToProcess(processHandler)

            val descriptor =
                RunContentDescriptor(consoleView, processHandler, consoleView.component, "Parsed Test Results")
            RunContentManager.getInstance(project)
                .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)

            Executors.newSingleThreadScheduledExecutor().schedule({
                val results = parseResultFile(file)
                ApplicationManager.getApplication().invokeLater {
                    processHandler.notifyTextAvailable(
                        "##teamcity[testSuiteStarted name='ParsedSuite']\n",
                        ProcessOutputTypes.STDOUT
                    )
                    for ((_, result) in results) {
                        processHandler.notifyTextAvailable(
                            "##teamcity[testStarted name='${result.name}']\n",
                            ProcessOutputTypes.STDOUT
                        )

                        fun escapeTc(s: String): String =
                            s.replace("|", "||")
                                .replace("'", "|'")
                                .replace("\n", "|n")
                                .replace("\r", "|r")
                                .replace("[", "|[")
                                .replace("]", "|]")

                        when (result.status) {
                            Status.FAILED -> processHandler.notifyTextAvailable(
                                "##teamcity[testFailed name='${result.name}' message='${escapeTc(result.failureMessage ?: "Failed")}' details='${
                                    escapeTc(
                                        result.failureDetails ?: ""
                                    )
                                }']\n",
                                ProcessOutputTypes.STDOUT
                            )

                            Status.ERROR -> processHandler.notifyTextAvailable(
                                "##teamcity[testFailed name='${result.name}' message='${escapeTc(result.errorMessage ?: "Error")}' details='${
                                    escapeTc(
                                        result.errorDetails ?: ""
                                    )
                                }']\n",
                                ProcessOutputTypes.STDOUT
                            )

                            Status.SKIPPED -> processHandler.notifyTextAvailable(
                                "##teamcity[testIgnored name='${result.name}' message='${escapeTc(result.skippedMessage ?: "Skipped")}']\n",
                                ProcessOutputTypes.STDOUT
                            )

                            else -> {}
                        }

                        processHandler.notifyTextAvailable(
                            "##teamcity[testFinished name='${result.name}']\n",
                            ProcessOutputTypes.STDOUT
                        )
                    }
                    processHandler.notifyTextAvailable(
                        "##teamcity[testSuiteFinished name='ParsedSuite']\n",
                        ProcessOutputTypes.STDOUT
                    )
                    processHandler.destroyProcess()
                }
            }, 500, TimeUnit.MILLISECONDS)
        }
    }

    private fun buildSshCommand(host: String, jmxInput: String): String = """
        ssh -o StrictHostKeyChecking=accept-new root@$host \
        "docker exec ots-17-17 mkdir -p /usr/local/workday-oms/logs/junit && \
        echo -e \"$jmxInput\" | java -jar /usr/local/bin/jmxterm-1.0-SNAPSHOT-uber.jar"
    """.trimIndent()

    private fun buildScpCommand(project: Project, host: String): String {
        val targetDir = project.basePath ?: System.getProperty("user.home")
        return "scp root@$host:/data/workdaydevqa/suv/suvots/logs/junit/* \"$targetDir\""
    }

    private fun notifyUser(project: Project): Notification {
//        val notification = NotificationGroupManager.getInstance()
//            .getNotificationGroup("OmsTest Notifications")
//            .createNotification("Running remote test...", NotificationType.INFORMATION)
//        notification.notify(project)
//        return notification
        return NotificationGroupManager.getInstance()
            .getNotificationGroup("OmsTest Notifications")
            .createNotification("Running remote test...", NotificationType.INFORMATION)
            .also { it.notify(project) }
//        val notification = NotificationGroupManager.getInstance()
//            .getNotificationGroup("run.notifications") // This is a known valid built-in ID
//            .createNotification("Running remote test...", NotificationType.INFORMATION)
//        notification.notify(project)        return notification
    }

    fun printTestResultLinks(project: Project, consoleView: ConsoleView) {
        val basePath = project.basePath ?: return
        val testFiles = listOf(
            "TEST-junit-jupiter.xml",
            "TEST-junit-platform-suite.xml"
        )

        for (fileName in testFiles) {
            val file = File(basePath, fileName)
            if (file.exists()) {
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
                if (virtualFile != null) {
                    consoleView.printHyperlink("Open $fileName") {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    }
                    consoleView.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
                } else {
                    consoleView.print("File not found: $fileName\n", ConsoleViewContentType.ERROR_OUTPUT)
                }
            }
        }
    }

    private fun runCommand(command: String, console: ConsoleView, title: String) {
        console.print("\n> $title\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        try {
            val process = ProcessBuilder("/bin/sh", "-c", command)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().lines().forEach { line ->
                console.print("$line\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }

            val exitCode = process.waitFor()
            console.print("Process exited with code $exitCode\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        } catch (e: Exception) {
            console.print("Error: ${e.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
        }
    }
}
