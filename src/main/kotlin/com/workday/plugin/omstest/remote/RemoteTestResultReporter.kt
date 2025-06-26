package com.workday.plugin.omstest.remote

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.workday.plugin.omstest.model.JUnitTestResult
import com.workday.plugin.omstest.run.RemoteTestProcessHandler

object RemoteTestResultReporter {

    fun reportToIntelliJ(results: Map<String, JUnitTestResult>, processHandler: ProcessHandler) {
        processHandler.notifyTextAvailable("##teamcity[testSuiteStarted name='DebugCheck']\n", ProcessOutputTypes.STDOUT)
        processHandler.notifyTextAvailable("##teamcity[testStarted name='dummyTest']\n", ProcessOutputTypes.STDOUT)
        processHandler.notifyTextAvailable("##teamcity[testFinished name='dummyTest']\n", ProcessOutputTypes.STDOUT)
        processHandler.notifyTextAvailable("##teamcity[testSuiteFinished name='DebugCheck']\n", ProcessOutputTypes.STDOUT)


//        results.values.groupBy { it.className }.forEach { (className, tests) ->
//            processHandler.notifyTextAvailable("##teamcity[testSuiteStarted name='$className']\n", ProcessOutputTypes.STDOUT)
//
//            tests.forEach { test ->
//                processHandler.notifyTextAvailable("##teamcity[testStarted name='${test.name}']\n", ProcessOutputTypes.STDOUT)
//
//                when (test.status) {
//                    JUnitTestResult.Status.FAILED, JUnitTestResult.Status.ERROR -> {
//                        val message = test.failureMessage ?: test.errorMessage ?: "Test failed"
//                        val details = (test.failureDetails ?: test.errorDetails ?: "")
//                            .replace("\n", "|n")
//                            .replace("'", "|'")
//                        processHandler.notifyTextAvailable(
//                            "##teamcity[testFailed name='${test.name}' message='$message' details='$details']\n",
//                            ProcessOutputTypes.STDOUT
//                        )
//                    }
//
//                    JUnitTestResult.Status.SKIPPED -> {
//                        val reason = test.skippedMessage ?: "Skipped"
//                        processHandler.notifyTextAvailable(
//                            "##teamcity[testIgnored name='${test.name}' message='$reason']\n",
//                            ProcessOutputTypes.STDOUT
//                        )
//                    }
//
//                    else -> { /* PASSED, nothing extra needed */ }
//                }
//
//                val durationMillis = (test.time * 1000).toInt()
//                processHandler.notifyTextAvailable(
//                    "##teamcity[testFinished name='${test.name}' duration='$durationMillis']\n",
//                    ProcessOutputTypes.STDOUT
//                )
//            }
//
//            processHandler.notifyTextAvailable("##teamcity[testSuiteFinished name='$className']\n", ProcessOutputTypes.STDOUT)
//        }

        (processHandler as? RemoteTestProcessHandler)?.finish(0)
    }
}