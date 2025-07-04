package com.workday.plugin.omstest.junit

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.ReRunLastTest
import com.workday.plugin.omstest.util.JunitProcessHandler
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

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
     */
    fun displayParsedResults(
        processHandler: ProcessHandler,
        path: String?,
        onDone: () -> Unit = {}
    ) {
        val logFile = File(path, TEST_JUNIT_JUPITER_XML)
        if (!logFile.exists()) {
            onDone()
            return
        }


        ApplicationManager.getApplication().executeOnPooledThread {
            val suite = JunitResultParser().parseTestSuite(logFile)
            ApplicationManager.getApplication().invokeLater {
                suite?.let { displayTestSuiteResult(it, processHandler) }
                onDone()
            }
        }
    }



    private fun displayTestSuiteResult(suite: TestSuite, processHandler: ProcessHandler) {

        processHandler.notifyTextAvailable(
            "After suite started\n",
            ProcessOutputTypes.STDOUT
        )
        suite.results.groupBy { it.className }
            .forEach { (className, results) ->
                val suiteName = className.substringAfterLast('.')
                processHandler.notifyTextAvailable(
                    "##teamcity[testSuiteStarted name='$suiteName']\n",
                    ProcessOutputTypes.STDOUT
                )
                displayResults(results.associateBy { it.name }, processHandler)
                processHandler.notifyTextAvailable(
                    "##teamcity[testSuiteFinished name='$suiteName']\n",
                    ProcessOutputTypes.STDOUT
                )
            }
    }

    private fun displayResults(results: Map<String, JunitTestResult>, processHandler: ProcessHandler) {
        fun escapeTc(s: String): String =
            s.replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]")

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
                "##teamcity[testFinished name='${result.name}' duration='${result.timeInMillisStr}']\n",
                ProcessOutputTypes.STDOUT
            )
        }
    }
}
