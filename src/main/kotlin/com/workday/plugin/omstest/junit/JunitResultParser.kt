package com.workday.plugin.omstest.junit

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses a JUnit XML result file and extracts test results.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class JunitResultParser {

//    fun parseResultFile(file: File): Map<String, JunitTestResult> {
//        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
//        val doc = docBuilder.parse(file)
//        val root = doc.documentElement
//        val testCases = root.getElementsByTagName("testcase")
//
//        val results = mutableMapOf<String, JunitTestResult>()
//        for (i in 0 until testCases.length) {
//            val testCaseElement = testCases.item(i) as Element
//            val result = parseTestCase(testCaseElement)
//            val key = "${result.className}#${result.name}"
//            results[key] = result
//        }
//        return results
//    }

    fun parseTestSuite(file: File): TestSuite? {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val suiteElement = document.getElementsByTagName("testsuite").item(0) as? Element ?: return null

        val testCases = suiteElement.getElementsByTagName("testcase")
        val results = mutableListOf<JunitTestResult>()
        for (i in 0 until testCases.length) {
            val testCaseElement = testCases.item(i) as Element
            results += parseTestCase(testCaseElement)
        }

        val time = suiteElement.getAttribute("time")

        return TestSuite(
            name = suiteElement.getAttribute("name"),
            tests = suiteElement.getAttribute("tests").toIntOrNull() ?: 0,
            skipped = suiteElement.getAttribute("skipped").toIntOrNull() ?: 0,
            failures = suiteElement.getAttribute("failures").toIntOrNull() ?: 0,
            errors = suiteElement.getAttribute("errors").toIntOrNull() ?: 0,
            timeMillisStr =  ( 1000.times(time.toDoubleOrNull()?:0.toDouble()) ).toInt().toString(),
            hostname = suiteElement.getAttribute("hostname"),
            timestamp = suiteElement.getAttribute("timestamp"),
            results = results,
            status = "NORMAL"
        )
    }

    private fun parseTestCase(testCaseElement: Element): JunitTestResult {
        val name = testCaseElement.getAttribute("name")
        val className = testCaseElement.getAttribute("classname")
        val time = testCaseElement.getAttribute("time")

        val systemOut = getElementTextContent(testCaseElement, "system-out")
        val systemErr = getElementTextContent(testCaseElement, "system-err")
        val failureMessage = getElementMessageAttribute(testCaseElement, "failure")
        val failureDetails = getElementTextContent(testCaseElement, "failure")
        val errorMessage = getElementMessageAttribute(testCaseElement, "error")
        val errorDetails = getElementTextContent(testCaseElement, "error")
        val skippedMessage = getElementTextContent(testCaseElement, "skipped")

        val status = when {
            errorMessage != null -> Status.ERROR
            failureMessage != null -> Status.FAILED
            skippedMessage != null -> Status.SKIPPED
            else -> Status.PASSED
        }

        return JunitTestResult(
            name = name,
            className = className,
            timeInMillisStr = ( 1000.times(time.toDoubleOrNull()?:0.toDouble()) ).toInt().toString(),
            status = status,
            failureMessage = failureMessage,
            failureDetails = failureDetails,
            errorMessage = errorMessage,
            errorDetails = errorDetails,
            skippedMessage = skippedMessage,
            systemOut = systemOut,
            systemErr = systemErr
        )
    }

    private fun getElementTextContent(parent: Element, tagName: String): String? {
        val nodeList = parent.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            val node = nodeList.item(0)
            return node?.textContent
        }
        return null
    }

    private fun getElementMessageAttribute(parent: Element, tagName: String): String? {
        val nodeList = parent.getElementsByTagName(tagName)
        if (nodeList.length > 0) {
            val node = nodeList.item(0) as? Element
            return node?.getAttribute("message")
        }
        return null
    }
}


enum class Status {
    PASSED, FAILED, SKIPPED, ERROR
}

/**
 * Represents a single JUnit test result.
 *
 * @property name The name of the test method.
 * @property className The fully qualified name of the test class.
 * @property timeInMillisStr The time taken to run the test, or null if not available.
 * @property status The status of the test (PASSED, FAILED, SKIPPED, ERROR).
 * @property failureMessage The message associated with a failure, if any.
 * @property failureDetails Additional details about the failure, if any.
 * @property errorMessage The message associated with an error, if any.
 * @property errorDetails Additional details about the error, if any.
 * @property skippedMessage The message associated with a skipped test, if any.
 * @property systemOut Standard output captured during the test execution.
 * @property systemErr Standard error output captured during the test execution.
 */
data class JunitTestResult(
    val name: String,
    val className: String,
    val timeInMillisStr: String? = null,
    val status: Status,
    val failureMessage: String? = null,
    val failureDetails: String? = null,
    val errorMessage: String? = null,
    val errorDetails: String? = null,
    val skippedMessage: String? = null,
    val systemOut: String? = null,
    val systemErr: String? = null
)

data class TestSuite(
    val name: String,
    val tests: Int,
    val skipped: Int,
    val failures: Int,
    val errors: Int,
    val timeMillisStr: String,
    val hostname: String?,
    val timestamp: String?,
    val status: String?,
    val results: List<JunitTestResult>
)