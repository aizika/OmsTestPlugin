package com.workday.plugin.omstest.execution


import com.intellij.execution.executors.DefaultRunExecutor.getRunExecutorInstance
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.PluginConstants
import com.workday.plugin.omstest.ui.TestResultPresenter
import com.workday.plugin.omstest.ui.UiContentDescriptor
import java.io.File
import java.nio.file.Files
import kotlin.io.path.Path

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

        // Delete test result file if exists
        val basePath = project.basePath ?: "."
        val parent = "$basePath${PluginConstants.LOCAL_RESULTS_DIR}"
        val fullPath = Path(parent + "/" + PluginConstants.TEST_RESULT_FILE_NAME)
        if (Files.exists(fullPath)) {
            Files.deleteIfExists(fullPath)
        }

        val junitDescriptor = UiContentDescriptor.createDescriptor(project, runTabName)
        val processHandler = junitDescriptor.getUiProcessHandler()
        processHandler.startNotify()

        fun log(msg: String) {
            processHandler.notifyTextAvailable("$msg\n", ProcessOutputTypes.STDOUT)
        }

        val xmlFile = File(parent, PluginConstants.TEST_RESULT_FILE_NAME)

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
                        processHandler.notifyTextAvailable(line + "\n", ProcessOutputTypes.STDOUT)
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
                        TestResultPresenter().displayParsedResults(processHandler, parent) {
                            log("Test results displayed")
                            processHandler.finish()
                        }
                    }
                } else {
                    log("XML result file not found after timeout in $parent")
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