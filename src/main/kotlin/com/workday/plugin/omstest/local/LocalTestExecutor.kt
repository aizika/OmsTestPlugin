package com.workday.plugin.omstest.local


import com.intellij.execution.executors.DefaultRunExecutor.getRunExecutorInstance
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.junit.JunitTestPanel
import com.workday.plugin.omstest.junit.ParsedResultConsole
import com.workday.plugin.omstest.util.JunitProcessHandler
import java.io.File

/**
 * Utility object for running external commands and displaying their output in an IntelliJ IDEA console tab.
 * Stores the last executed command and label for rerun functionality.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
object LocalTestExecutor {

    fun runLocalCommand(
        project: Project?,
        runTabName: String,
        targetName: String,
        processHandler: JunitProcessHandler
    ) {
        if (project == null) return

        fun log(msg: String) {
            processHandler.pushOutput(msg + "\n", ProcessOutputTypes.STDOUT)
        }

        log("🚀 runLocalCommand started")
        log("🔧 targetName: $targetName")

        val console = ParsedResultConsole()
        console.initAndShow(project, processHandler)
        val consoleView = console.consoleView!!

        val path = project.basePath + "/build/test-results/legacy-xml"
        val xmlFile = File(path, "TEST-junit-jupiter.xml")

        processHandler.start()

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val command = listOf(
                    "./gradlew",
                    targetName,
                    ":runTestJmx",
                    "-s"
                )
                val process = ProcessBuilder(command)
                    .directory(File(project.basePath ?: "."))
                    .redirectErrorStream(true)
                    .start()

                log("Gradle process started")

                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        processHandler.pushOutput(line + "\n", ProcessOutputTypes.STDOUT)
                    }
                }

                val exitCode = process.waitFor()
                log("Gradle process exited with code $exitCode")

                // Wait for XML file to appear
                var attempts = 20
                while (!xmlFile.exists() && attempts-- > 0) {
                    Thread.sleep(300)
                }

                if (xmlFile.exists()) {
                    log("📄 XML file found, parsing test results")
                    ApplicationManager.getApplication().invokeLater {
                        val junitTestPanel = JunitTestPanel()
                        junitTestPanel.displayParsedResults(processHandler, path) {
                            log("Test results displayed")
                            processHandler.finish()
                        }
                    }
                } else {
                    log("XML resul file $path not found after timeout")
                    processHandler.finish()
                }

            } catch (e: Exception) {
                log("Exception: ${e.message}")
                processHandler.finish()
            }
        }

        val descriptor = RunContentDescriptor(
            consoleView,
            processHandler,
            consoleView.component,
            runTabName
        )
        RunContentManager.getInstance(project)
            .showRunContent(getRunExecutorInstance(), descriptor)
    }
}