package com.workday.plugin.omstest.junit

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import java.io.File
import java.io.OutputStream
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
        val logFile = File(project.basePath, TEST_JUNIT_JUPITER_XML)
        if (!logFile.exists()) return

        ApplicationManager.getApplication().invokeLater {
            val processHandler = object : ProcessHandler() {
                override fun destroyProcessImpl() = notifyProcessTerminated(0)
                override fun detachProcessImpl() = notifyProcessDetached()
                override fun detachIsDefault(): Boolean = false
                override fun getProcessInput(): OutputStream? = null
            }
            ParsedResultConsole().initAndShow(project, processHandler)
            if (true) {
                Executors.newSingleThreadScheduledExecutor().schedule({
                    val results = JunitResultParser().parseResultFile(logFile)
                    ApplicationManager.getApplication().invokeLater {
                        displayResults(results, processHandler)
                    }
                }, 500, TimeUnit.MILLISECONDS)
            }
            if (false) {
                Executors.newSingleThreadScheduledExecutor().schedule({
                    val suite = JunitResultParser().parseTestSuite(logFile)
                    ApplicationManager.getApplication().invokeLater {
                        suite?.let { displayTestSuiteResult(it, processHandler) }
                    }
                }, 500, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun displayTestSuiteResult(suite: TestSuite, processHandler: ProcessHandler) {
        fun escapeTc(s: String): String =
            s.replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]")

        val suiteName = escapeTc(suite.name)

        // Start the test suite
        processHandler.notifyTextAvailable(
            "##teamcity[testSuiteStarted name='$suiteName']\n",
            ProcessOutputTypes.STDOUT
        )

        // Display all test results
        val results = suite.results.associateBy { it.name }
        displayResults(results, processHandler)

        // Optionally, you could log summary metadata if desired:
        // (not standard TeamCity format, just informative text)
        processHandler.notifyTextAvailable(
            "##teamcity[message text='Suite Summary: tests=${suite.tests}, failures=${suite.failures}, errors=${suite.errors}, skipped=${suite.skipped}, time=${suite.time}, status=${suite.status}, duration=123 ']\n",
            ProcessOutputTypes.STDOUT
        )

        // End the test suite
        processHandler.notifyTextAvailable(
            "##teamcity[testSuiteFinished name='${suite.name}' duration=123.456 ]\n",
            ProcessOutputTypes.STDOUT
        )

        processHandler.destroyProcess()
    }

    private fun displayResults(results: Map<String, JunitTestResult>, processHandler: ProcessHandler) {
        fun escapeTc(s: String): String =
            s.replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]")

//        processHandler.notifyTextAvailable(
//            "##teamcity[testSuiteStarted name='PPPPPPP']\n",
//            ProcessOutputTypes.STDOUT
//        )

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

//            processHandler.notifyTextAvailable(
//                "##teamcity[message='${result.name}'  duration='987.431']\n",
//                ProcessOutputTypes.STDOUT
//            )
            processHandler.notifyTextAvailable(
                "##teamcity[testFinished name='${result.name}' duration='${result.timeInMillisStr}']\n",
                ProcessOutputTypes.STDOUT
            )
        }

//        processHandler.notifyTextAvailable(
//            "##teamcity[testSuiteFinished name='ParsedSuite' duration='1.23']\n",
//            ProcessOutputTypes.STDOUT
//        )
        processHandler.destroyProcess()
    }
}
