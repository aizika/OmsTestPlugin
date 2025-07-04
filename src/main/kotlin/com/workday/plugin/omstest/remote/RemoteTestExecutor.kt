package com.workday.plugin.omstest.remote

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.ReRunLastTest
import com.workday.plugin.omstest.junit.JunitTestPanel
import com.workday.plugin.omstest.junit.ParsedResultConsole
import com.workday.plugin.omstest.util.JunitProcessHandler
import com.workday.plugin.omstest.util.LastTestStorage
import javax.swing.JPanel
import java.awt.BorderLayout
/**
 * Utility object for running remote tests on a specified host.
 * Provides methods to run test classes and methods with JMX parameters.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
object RemoteTestExecutor {

    fun runRemoteTest(
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


    fun getResultConsoleView(project: Project, processHandler: ProcessHandler): ConsoleView {
        val consoleView = ParsedResultConsole().createConsoleView(project, processHandler)

        // Attach process listeners
        val resultPath = project.basePath + "/build/test-results/legacy-xml"
        processHandler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(event: ProcessEvent) {
                val junitTestPanel = JunitTestPanel()
                junitTestPanel.displayParsedResults(processHandler, resultPath) {
                    // Optional cleanup
                }
            }
        })
        processHandler.startNotify()

        // 🔧 Create custom toolbar with a Re-run button
        val toolbarGroup = DefaultActionGroup().apply {
            add(ReRunLastTest())  // Make sure this action is defined and registered in plugin.xml
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("OmsToolbar", toolbarGroup, false)
        toolbar.setTargetComponent(consoleView.component)

        // Wrap the console and toolbar into a JPanel
        val consolePanel = JPanel(BorderLayout())
        consolePanel.add(toolbar.component, BorderLayout.WEST)
        consolePanel.add(consoleView.component, BorderLayout.CENTER)

        val descriptor = RunContentDescriptor(
            consoleView,
            processHandler,
            consolePanel,  // Use the panel with toolbar
            "OMS Test Results"
        )

        RunContentManager.getInstance(project)
            .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)

        return consoleView
    }

    fun runTestWithHost(
        project: Project,
        fqTestName: String,
        jmxParams: String,
        host: String,
        runTabName: String
    ) {
        LastTestStorage.setRemote(host, fqTestName, jmxParams, runTabName)
        val processHandler = JunitProcessHandler()
        val consoleView = getResultConsoleView(project, processHandler)

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
            val junitTestPanel = JunitTestPanel()
            runRemoteCommand(sshCommand, consoleView, processHandler, "Running test on $host")
            runRemoteCommand(scpCommand, consoleView, processHandler, "Fetching logs from $host")

            notification.expire()
            junitTestPanel.displayParsedResults(processHandler, project.basePath) {
                processHandler.finish()
            }

        }
        println("[TEST-PANEL] Finished displayTestSuiteResult(...)")
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
        return NotificationGroupManager.getInstance()
            .getNotificationGroup("OmsTest Notifications")
            .createNotification("Running remote test...", NotificationType.INFORMATION)
            .also { it.notify(project) }
    }

    private fun runRemoteCommand(command: String, console: ConsoleView, processHandler: JunitProcessHandler, title: String) {
        console.print("\n> $title\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        processHandler.pushOutput("$title\n", ProcessOutputTypes.STDOUT)
        try {
            val process = ProcessBuilder("/bin/sh", "-c", command)
                .redirectErrorStream(true)
                .start()

            process.inputStream.bufferedReader().lines().forEach { line ->
                processHandler.pushOutput(line + "\n", ProcessOutputTypes.STDOUT)
            }

            val exitCode = process.waitFor()
            console.print("Process exited with code $exitCode\n", ConsoleViewContentType.SYSTEM_OUTPUT)
            processHandler.pushOutput("Process exited with code $exitCode\n", ProcessOutputTypes.STDOUT)
        } catch (e: Exception) {
            console.print("Error: ${e.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
            processHandler.pushOutput("Error: ${e.message}\n", ProcessOutputTypes.STDERR)
        }
    }
}
