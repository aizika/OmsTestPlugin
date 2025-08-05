package com.workday.plugin.testrunner.ui;


import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

            if (suiteElement == null) return null;

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
        } catch (Exception e) {
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
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }
}

enum Status {
    PASSED, FAILED, SKIPPED, ERROR
}

class TestMethodResult {
    public final String name;
    public final String className;
    public final String timeInMillisStr;
    public final Status status;
    public final String failureMessage;
    public final String failureDetails;
    public final String errorMessage;
    public final String errorDetails;
    public final String skippedMessage;
    public final String systemOut;
    public final String systemErr;

    public String getTimeInMillisStr() {
        return timeInMillisStr;
    }

    public Status getStatus() {
        return status;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public String getFailureDetails() {
        return failureDetails;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public String getSkippedMessage() {
        return skippedMessage;
    }

    public String getSystemOut() {
        return systemOut;
    }

    public String getSystemErr() {
        return systemErr;
    }

    public TestMethodResult(String name, String className, String timeInMillisStr, Status status,
                            String failureMessage, String failureDetails,
                            String errorMessage, String errorDetails,
                            String skippedMessage, String systemOut, String systemErr) {
        this.name = name;
        this.className = className;
        this.timeInMillisStr = timeInMillisStr;
        this.status = status;
        this.failureMessage = failureMessage;
        this.failureDetails = failureDetails;
        this.errorMessage = errorMessage;
        this.errorDetails = errorDetails;
        this.skippedMessage = skippedMessage;
        this.systemOut = systemOut;
        this.systemErr = systemErr;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }
}

class TestSuiteResult {

    public String getName() {
        return name;
    }

    public int getTests() {
        return tests;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getFailures() {
        return failures;
    }

    public int getErrors() {
        return errors;
    }

    public String getTimeMillisStr() {
        return timeMillisStr;
    }

    public String getHostname() {
        return hostname;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getStatus() {
        return status;
    }

    public List<TestMethodResult> getResults() {
        return results;
    }

    public final String name;
    public final int tests;
    public final int skipped;
    public final int failures;
    public final int errors;
    public final String timeMillisStr;
    public final String hostname;
    public final String timestamp;
    public final String status;
    public final List<TestMethodResult> results;

    public TestSuiteResult(String name, int tests, int skipped, int failures, int errors,
                           String timeMillisStr, String hostname, String timestamp,
                           String status, List<TestMethodResult> results) {
        this.name = name;
        this.tests = tests;
        this.skipped = skipped;
        this.failures = failures;
        this.errors = errors;
        this.timeMillisStr = timeMillisStr;
        this.hostname = hostname;
        this.timestamp = timestamp;
        this.status = status;
        this.results = results;
    }
}
