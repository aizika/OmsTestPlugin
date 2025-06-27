package com.workday.plugin.omstest.parser

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

fun parseResultFile(file: File): Map<String, JunitTestResult> {
    val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val doc = docBuilder.parse(file)
    val root = doc.documentElement
    val testCases = root.getElementsByTagName("testcase")

    val results = mutableMapOf<String, JunitTestResult>()
    for (i in 0 until testCases.length) {
        val testCaseElement = testCases.item(i) as Element
        val result = parseTestCase(testCaseElement)
        val key = "${result.className}#${result.name}"
        results[key] = result
    }
    return results
}

private fun parseTestCase(testCaseElement: Element): JunitTestResult {
    val name = testCaseElement.getAttribute("name")
    val className = testCaseElement.getAttribute("classname")
    val time = testCaseElement.getAttribute("time")

    val systemOut = getElementTextContent(testCaseElement, "system-out")
    val systemErr = getElementTextContent(testCaseElement, "system-err")
    val failureMessage = getElementAttribute(testCaseElement, "failure", "message")
    val failureDetails = getElementTextContent(testCaseElement, "failure")
    val errorMessage = getElementAttribute(testCaseElement, "error", "message")
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
        time = time,
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

private fun getElementAttribute(parent: Element, tagName: String, attribute: String): String? {
    val nodeList = parent.getElementsByTagName(tagName)
    if (nodeList.length > 0) {
        val node = nodeList.item(0) as? Element
        return node?.getAttribute(attribute)
    }
    return null
}


enum class Status {
    PASSED, FAILED, SKIPPED, ERROR
}

data class JunitTestResult(
    val name: String,
    val className: String,
    val time: String?,
    val status: Status,
    val failureMessage: String? = null,
    val failureDetails: String? = null,
    val errorMessage: String? = null,
    val errorDetails: String? = null,
    val skippedMessage: String? = null,
    val systemOut: String? = null,
    val systemErr: String? = null
)