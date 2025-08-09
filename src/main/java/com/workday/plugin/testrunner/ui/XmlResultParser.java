package com.workday.plugin.testrunner.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Parses an XML result file and extracts test results.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class XmlResultParser {

    public TestSuiteResult parseTestSuite(File file) {
        try {
            Element suiteElement = (Element) DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName("testsuite")
                .item(0);

            if (suiteElement == null) {
                return null;
            }

            NodeList testCases = suiteElement.getElementsByTagName("testcase");
            List<TestMethodResult> results = new ArrayList<>();
            for (int i = 0; i < testCases.getLength(); i++) {
                Element testCaseElement = (Element) testCases.item(i);
                results.add(parseTestCase(testCaseElement));
            }

            String timeStr = suiteElement.getAttribute("time");
            int millis = (int) (1000 * parseTime(timeStr));

            return new TestSuiteResult(
                suiteElement.getAttribute("name"),
                parseInt(suiteElement.getAttribute("tests")),
                parseInt(suiteElement.getAttribute("skipped")),
                parseInt(suiteElement.getAttribute("failures")),
                parseInt(suiteElement.getAttribute("errors")),
                Integer.toString(millis),
                suiteElement.getAttribute("hostname"),
                suiteElement.getAttribute("timestamp"),
                "NORMAL",
                results
            );
        }
        catch (Exception e) {
            return null;
        }
    }

    private TestMethodResult parseTestCase(Element testCaseElement) {
        String name = testCaseElement.getAttribute("name");
        String className = testCaseElement.getAttribute("classname");
        String timeStr = testCaseElement.getAttribute("time");
        int millis = (int) (1000 * parseTime(timeStr));

        String systemOut = getElementTextContent(testCaseElement, "system-out");
        String systemErr = getElementTextContent(testCaseElement, "system-err");
        String failureMessage = getElementMessageAttribute(testCaseElement, "failure");
        String failureDetails = getElementTextContent(testCaseElement, "failure");
        String errorMessage = getElementMessageAttribute(testCaseElement, "error");
        String errorDetails = getElementTextContent(testCaseElement, "error");
        String skippedMessage = getElementTextContent(testCaseElement, "skipped");

        Status status = errorMessage != null ? Status.ERROR
            : failureMessage != null ? Status.FAILED
                : skippedMessage != null ? Status.SKIPPED
                    : Status.PASSED;

        return new TestMethodResult(
            name,
            className,
            Integer.toString(millis),
            status,
            failureMessage,
            failureDetails,
            errorMessage,
            errorDetails,
            skippedMessage,
            systemOut,
            systemErr
        );
    }

    private String getElementTextContent(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private String getElementMessageAttribute(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0 && nodeList.item(0) instanceof Element) {
            return ((Element) nodeList.item(0)).getAttribute("message");
        }
        return null;
    }

    private double parseTime(String timeStr) {
        try {
            return Double.parseDouble(timeStr);
        }
        catch (Exception e) {
            return 0.0;
        }
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return 0;
        }
    }
}

enum Status {
    PASSED,
    FAILED,
    SKIPPED,
    ERROR
}

record TestMethodResult(String name, String className, String timeInMillisStr, Status status, String failureMessage,
                        String failureDetails, String errorMessage, String errorDetails, String skippedMessage,
                        String systemOut, String systemErr) {

}

