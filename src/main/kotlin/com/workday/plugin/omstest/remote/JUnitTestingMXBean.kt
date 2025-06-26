package com.workday.plugin.omstest.remote

interface JUnitTestingMXBean {
    fun executeTestSuite(
        testMethod: String,
        testClass: String,
        testPackage: String,
        testConcurrent: String,
        testCategory: String,
        toDir: String
    ): String
}