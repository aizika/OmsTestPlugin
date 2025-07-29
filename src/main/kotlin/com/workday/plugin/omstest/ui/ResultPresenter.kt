package com.workday.plugin.omstest.ui

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import java.io.File

private const val TEST_JUNIT_JUPITER_XML = "TEST-junit-jupiter.xml"

/**
 * Utility class for displaying parsed JUnit test results in the IntelliJ test runner console.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class TestResultPresenter {

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


        /**
         * Asynchronously parses the test results from the specified XML file using XmlResultParser
         * and displays them in the console.
         * Uses ApplicationManager to run the parsing in a background thread and updates the UI on the EDT.
         */
        ApplicationManager.getApplication().executeOnPooledThread {
            val suite = XmlResultParser().parseTestSuite(logFile)
            ApplicationManager.getApplication().invokeLater {
                suite?.let { displayTestSuiteResult(it, processHandler) }
                onDone()
            }
        }
    }


    private fun displayTestSuiteResult(suite: TestSuiteResult, processHandler: ProcessHandler) {
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

    private fun displayResults(results: Map<String, TestMethodResult>, processHandler: ProcessHandler) {
        fun escapeTc(s: String): String =
            s.replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]")

        val sortedEntries = results.toSortedMap()
        for ((_, result) in sortedEntries) {
            val escapedName = escapeTc(result.name)
            val strippedBracketsName = result.name.replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            val location = "java:${result.className}#$strippedBracketsName"
            processHandler.notifyTextAvailable(
                "##teamcity[testStarted name='$escapedName' captureStandardOutput='true' locationHint='$location']\n",
                ProcessOutputTypes.STDOUT
            )

            when (result.status) {
                Status.FAILED -> processHandler.notifyTextAvailable(
                    "##teamcity[testFailed name='$escapedName' message='${escapeTc(result.failureMessage ?: "Failed")}' details='${
                        escapeTc(
                            result.failureDetails ?: ""
                        )
                    }']\n",
                    ProcessOutputTypes.STDOUT
                )

                Status.ERROR -> processHandler.notifyTextAvailable(
                    "##teamcity[testFailed name='$escapedName' message='${escapeTc(result.errorMessage ?: "Error")}' details='${
                        escapeTc(
                            result.errorDetails ?: ""
                        )
                    }']\n",
                    ProcessOutputTypes.STDOUT
                )

                Status.SKIPPED -> processHandler.notifyTextAvailable(
                    "##teamcity[testIgnored name='$escapedName' message='${escapeTc(result.skippedMessage ?: "Skipped")}']\n",
                    ProcessOutputTypes.STDOUT
                )

                else -> {}
            }
            processHandler.notifyTextAvailable(
                "##teamcity[testFinished name='$escapedName' duration='${result.timeInMillisStr}']\n",
                ProcessOutputTypes.STDOUT
            )
        }
    }
}
