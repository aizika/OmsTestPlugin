package com.workday.plugin.omstest.util

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import org.w3c.dom.Element
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

object XmlLogFormatter {

    fun prettyPrintXml(rawXml: String): String {
        return try {
            val transformer = TransformerFactory.newInstance().newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
                setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            }

            val source = StreamSource(StringReader(rawXml))
            val resultWriter = StringWriter()
            transformer.transform(source, StreamResult(resultWriter))
            resultWriter.toString()
        } catch (e: Exception) {
            "// Failed to pretty-print XML: ${e.message}\n$rawXml"
        }
    }

    fun summarizeTestResults(logFile: File, console: ConsoleView) {
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(logFile)
            val testcases = doc.getElementsByTagName("testcase")

            for (i in 0 until testcases.length) {
                val el = testcases.item(i) as? Element ?: continue
                val name = el.getAttribute("name")
                val classname = el.getAttribute("classname")
                val time = el.getAttribute("time")
                val hasFailure = el.getElementsByTagName("failure").length > 0

                val status = if (hasFailure) "❌ FAILED" else "✅ PASSED"
                console.print("$status  $classname.$name (${time}s)\n", ConsoleViewContentType.NORMAL_OUTPUT)
            }
        } catch (e: Exception) {
            console.print("Error reading test results: ${e.message}\n", ConsoleViewContentType.ERROR_OUTPUT)
        }
    }
}