package com.workday.plugin.omstest.local


import com.intellij.execution.executors.DefaultRunExecutor.getRunExecutorInstance
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.junit.JunitTestPanel
import com.workday.plugin.omstest.junit.JunitDescriptor
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
        targetName: String
    ) {
        if (project == null) return

        val junitDescriptor = JunitDescriptor.createDescriptor(project, runTabName)
        val processHandler = junitDescriptor.getMyProcessHandler()

        fun log(msg: String) {
            processHandler.pushOutput("$msg\n", ProcessOutputTypes.STDOUT)
        }


        val basePath = project.basePath ?: "."
        val path = "$basePath/build/test-results/legacy-xml"
        val xmlFile = File(path, "TEST-junit-jupiter.xml")

        RunContentManager.getInstance(project)
            .showRunContent(getRunExecutorInstance(), junitDescriptor)

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val command = listOf("./gradlew", targetName, ":runTestJmx", "-s")
                val process = ProcessBuilder(command)
                    .directory(File(basePath))
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

                val startTime = System.currentTimeMillis()
                while (!xmlFile.exists() && System.currentTimeMillis() - startTime < 6000) {
                    Thread.sleep(300)
                }

                if (xmlFile.exists()) {
                    log("XML file found, parsing test results")
                    ApplicationManager.getApplication().invokeLater {
                        val junitTestPanel = JunitTestPanel()
                        junitTestPanel.displayParsedResults(processHandler, path) {
                            log("Test results displayed")
                            processHandler.finish()
                        }
                    }
                } else {
                    log("XML result file not found after timeout in $path")
                    processHandler.finish()
                }

            } catch (e: Exception) {
                log("Exception: ${e.message}")
                log(e.stackTraceToString())
                processHandler.finish()
            }
        }

    }

}