package com.workday.plugin.omstest.junit

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
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
    fun displayParsedResults(project: Project, processHandler: ProcessHandler, onDone: () -> Unit = {}) {
        val logFile = File(project.basePath, TEST_JUNIT_JUPITER_XML)
        if (!logFile.exists()) {
            onDone()
            return
        }

        AppExecutorUtil.getAppScheduledExecutorService().schedule({
            val suite = JunitResultParser().parseTestSuite(logFile)
            ApplicationManager.getApplication().invokeLater {
                suite?.let { displayTestSuiteResult(it, processHandler) }
                onDone() // ✅ Finish only after display
            }
        }, 500, TimeUnit.MILLISECONDS)
    }
//    fun displayParsedResults(project: Project, processHandler: ProcessHandler) {
//        val logFile = File(project.basePath, TEST_JUNIT_JUPITER_XML)
//        if (!logFile.exists()) return
//
//        AppExecutorUtil.getAppScheduledExecutorService().schedule({
//            val suite = JunitResultParser().parseTestSuite(logFile)
//            ApplicationManager.getApplication().invokeLater {
//                suite?.let { displayTestSuiteResult(it, processHandler) }
//            }
//        }, 500, TimeUnit.MILLISECONDS)
//    }

    fun emitTestSuiteStarted(suiteName: String, processHandler: ProcessHandler) {
        processHandler.notifyTextAvailable(
            "##teamcity[testSuiteStarted name='$suiteName']\n",
            ProcessOutputTypes.STDOUT
        )
    }
    fun emitTestSuiteFinished(suiteName: String, processHandler: ProcessHandler) {
        processHandler.notifyTextAvailable(
            "##teamcity[testSuiteFinished name='$suiteName']\n",
            ProcessOutputTypes.STDOUT
        )
    }

    private fun displayTestSuiteResult(suite: TestSuite, processHandler: ProcessHandler) {
        val suiteName = "AA"

//        emitTestSuiteStarted(suiteName, processHandler)

        processHandler.notifyTextAvailable(
            "After suite started\n",
            ProcessOutputTypes.STDOUT
        )

        val results = suite.results.associateBy { it.name }
        displayResults(results, processHandler, {
            extracted(processHandler, suite, suiteName)
            processHandler.destroyProcess()
        })

    }

    private fun extracted(
        processHandler: ProcessHandler,
        suite: TestSuite,
        suiteName: String
    ) {
        processHandler.notifyTextAvailable(
            "Before suite finished\n",
            ProcessOutputTypes.STDOUT
        )

        processHandler.notifyTextAvailable(
            "##teamcity[message text='Suite Summary: tests=${suite.tests}, failures=${suite.failures}, errors=${suite.errors}, skipped=${suite.skipped}, time=${suite.timeMillisStr}, status=${suite.status}, duration=123 ']\n",
            ProcessOutputTypes.STDOUT
        )

        //        emitTestSuiteFinished(suiteName, processHandler)
        processHandler.notifyTextAvailable(
            "##teamcity[testSuiteFinished name='$suiteName']\n",
            ProcessOutputTypes.STDOUT
        )
    }

    private fun displayResults(results: Map<String, JunitTestResult>, processHandler: ProcessHandler, onDone: () -> Unit= {}) {
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
            processHandler.notifyTextAvailable(
                "After test started\n",
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
                "Before test finished\n",
                ProcessOutputTypes.STDOUT
            )

            processHandler.notifyTextAvailable(
                "##teamcity[testFinished name='${result.name}' duration='${result.timeInMillisStr}']\n",
                ProcessOutputTypes.STDOUT
            )
            onDone()
        }

//        processHandler.destroyProcess()
    }
    val teamCityMessages = listOf(
        // Suite start
        "##teamcity[testSuiteStarted name='Test Suite zero']",
        "##teamcity[testSuiteStarted name='Test Suite 1']",
        "##teamcity[testSuiteStarted name='Test Suite A']",

        // TEST_1_A start
        "##teamcity[testStarted name='Test 1.A' captureStandardOutput='false']",
//        "##teamcity[flowStarted flowId='mainFlow-1a']",

        // Subtest 1
        "##teamcity[testStarted name='Test 1.A, Subtest 1' captureStandardOutput='false']",
//        "##teamcity[flowStarted flowId='subFlow1-1a' parent='mainFlow-1a']",
//        "##teamcity[flowFinished flowId='subFlow1-1a']",
        "##teamcity[testFinished name='Test 1.A, Subtest 1' duration='1000']",

        // Subtest 2
        "##teamcity[testStarted name='Test 1.A, Subtest 2' captureStandardOutput='false']",
//        "##teamcity[flowStarted flowId='subFlow2-1a' parent='mainFlow-1a']",
//        "##teamcity[flowFinished flowId='subFlow2-1a']",
        "##teamcity[testFinished name='Test 1.A, Subtest 2' duration='1000']",

        // End TEST_1_A
//        "##teamcity[flowFinished flowId='mainFlow-1a']",
        "##teamcity[testFinished name='Test 1.A' duration='3000']",

        // Suite end
        "##teamcity[testSuiteFinished name='Test Suite A']",
        "##teamcity[testSuiteFinished name='Test Suite 1']",
        "##teamcity[testSuiteFinished name='Test Suite zero']"
    )
    fun displayDummy(project: Project, processHandler: ProcessHandler) {
        emitTeamCityMessages(processHandler, teamCityMessages)
    }
    fun emitTeamCityMessages(processHandler: ProcessHandler, messages: List<String>) {
        for (line in messages) {
            processHandler.notifyTextAvailable(line + "\n", ProcessOutputTypes.STDOUT)
        }
    }
}
