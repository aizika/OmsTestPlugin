package com.workday.plugin.omstest.model

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path
import kotlin.io.path.inputStream

data class JUnitTestResult(
    val name: String,
    val className: String,
    val time: Double,
    val status: Status,
    val failureMessage: String? = null,
    val failureDetails: String? = null,
    val errorMessage: String? = null,
    val errorDetails: String? = null,
    val skippedMessage: String? = null,
    val systemOut: String? = null,
    val systemErr: String? = null
) {
    enum class Status { PASSED, FAILED, ERROR, SKIPPED }

    companion object {
        fun parseResultFile(filePath: String): Map<String, JUnitTestResult> {
            val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            val doc = Path(filePath).inputStream().use { docBuilder.parse(it) }

            return doc.getElementsByTagName("testcase").let { nodes ->
                (0 until nodes.length).associate { i ->
                    val testCaseElement = nodes.item(i) as Element
                    val result = parseTestCase(testCaseElement)
                    ("${result.className}#${result.name}") to result
                }
            }
        }

        private fun parseTestCase(el: Element): JUnitTestResult {
            fun text(tag: String) = el.getElementsByTagName(tag).item(0)?.textContent?.trim()
            fun attr(tag: String, attr: String) = (el.getElementsByTagName(tag).item(0) as? Element)?.getAttribute(attr)

            return JUnitTestResult(
                name = el.getAttribute("name"),
                className = el.getAttribute("classname"),
                time = el.getAttribute("time").toDoubleOrNull() ?: 0.0,
                status = when {
                    attr("failure", "message") != null -> Status.FAILED
                    attr("error", "message") != null -> Status.ERROR
                    text("skipped") != null -> Status.SKIPPED
                    else -> Status.PASSED
                },
                failureMessage = attr("failure", "message"),
                failureDetails = text("failure"),
                errorMessage = attr("error", "message"),
                errorDetails = text("error"),
                skippedMessage = text("skipped"),
                systemOut = text("system-out"),
                systemErr = text("system-err")
            )
        }
    }
}