package com.workday.plugin.omstest.junit

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TEST_JUNIT_JUPITER_XML = "TEST-junit-jupiter.xml"

/**
 * A panel for displaying parsed JUnit test results in the IntelliJ test runner console.
 * This class reads a JMX test result file, parses it, and displays the results in a structured format
 * as if they were real JUnit test results.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class JunitTestPanel {

    /**
     * Displays parsed JUnit test results from a file in the IntelliJ test runner console.
     *
     * @param project The current IntelliJ project.
     */
    fun displayParsedResults(project: Project) {
        val file = File(project.basePath, TEST_JUNIT_JUPITER_XML)
        if (!file.exists()) return

        ApplicationManager.getApplication().invokeLater {
            val (consoleView, processHandler) = ParsedResultConsole().createConsole1(project)

            showConsole(project, consoleView, processHandler)

            Executors.newSingleThreadScheduledExecutor().schedule({
                val results = JunitResultParser().parseResultFile(file)
                ApplicationManager.getApplication().invokeLater {
                    displayResultsToConsole(results, processHandler)
                }
            }, 500, TimeUnit.MILLISECONDS)
        }
    }

    private fun showConsole(project: Project, consoleView: ConsoleView, processHandler: ProcessHandler) {
        val descriptor = RunContentDescriptor(consoleView, processHandler, consoleView.component, "Parsed Test Results")
        RunContentManager.getInstance(project)
            .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)
    }

    private fun displayResultsToConsole(results: Map<String, JunitTestResult>, processHandler: ProcessHandler) {
        fun escapeTc(s: String): String =
            s.replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]")

        processHandler.notifyTextAvailable(
            "##teamcity[testSuiteStarted name='ParsedSuite']\n",
            ProcessOutputTypes.STDOUT
        )

        for ((_, result) in results) {
            processHandler.notifyTextAvailable(
                "##teamcity[testStarted name='${result.name}']\n",
                ProcessOutputTypes.STDOUT
            )

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
}
