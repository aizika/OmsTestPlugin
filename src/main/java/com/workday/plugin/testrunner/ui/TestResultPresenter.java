package com.workday.plugin.testrunner.ui;

import static com.workday.plugin.testrunner.common.Locations.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.intellij.openapi.application.ApplicationManager;

/**
 * Utility class for displaying parsed JUnit test results in the IntelliJ test runner console.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class TestResultPresenter {

    public void displayParsedResults(UiContentDescriptor.UiProcessHandler processHandler) {
        File logFile = new File(getBasePath(), TEST_JUNIT_JUPITER_XML);
        if (!logFile.exists()) {
            processHandler.finish(1);
            return;
        }

        TestSuiteResult suite = new XmlResultParser().parseTestSuite(logFile);
        if (suite == null) {
            processHandler.log("Warning: could not parse test results from " + logFile.getAbsolutePath());
            processHandler.finish(1);
            return;
        }

        ApplicationManager.getApplication().invokeLater(() -> {
            displayTestSuiteResult(suite, processHandler);
            int exitCode = (suite.failures() > 0 || suite.errors() > 0) ? 1 : 0;
            processHandler.finish(exitCode);
        });
    }

    private void displayTestSuiteResult(TestSuiteResult suite, UiContentDescriptor.UiProcessHandler processHandler) {
        Map<String, List<TestMethodResult>> grouped = suite.results().stream()
                .collect(Collectors.groupingBy(TestMethodResult::className));

        for (Map.Entry<String, List<TestMethodResult>> entry : grouped.entrySet()) {
            String className = entry.getKey();
            List<TestMethodResult> results = entry.getValue();
            String suiteName = className.substring(className.lastIndexOf('.') + 1);
            String location = "java:" + className;

            processHandler.log(
                    "##teamcity[testSuiteStarted name='" + suiteName + "' locationHint='" + location + "']");

            Map<String, TestMethodResult> resultMap = results.stream()
                    .collect(Collectors.toMap(TestMethodResult::name, r -> r));
            displayResults(resultMap, processHandler);

            processHandler.log(
                    "##teamcity[testSuiteFinished name='" + suiteName + "']");
        }
    }

    private void displayResults(Map<String, TestMethodResult> results, UiContentDescriptor.UiProcessHandler processHandler) {
        results.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .forEach(result -> {
                    String escapedName = escapeTc(result.name());
                    String strippedBracketsName = result.name().replaceAll("\\(.*?\\)|\\[.*?]", "");
                    String location = "java:" + result.className() + "#" + strippedBracketsName;

                    processHandler.log(
                            "##teamcity[testStarted name='" + escapedName + "' captureStandardOutput='true' locationHint='"
                                    + location + "']");

                    switch (result.status()) {
                        case FAILED:
                            processHandler.log(
                                    String.format("##teamcity[testFailed name='%s' message='%s' details='%s']",
                                            escapedName,
                                            escapeTc(defaultIfNull(result.failureMessage(), "Failed")),
                                            escapeTc(defaultIfNull(result.failureDetails(), ""))));
                            break;
                        case ERROR:
                            processHandler.log(
                                    String.format("##teamcity[testFailed name='%s' message='%s' details='%s']",
                                            escapedName,
                                            escapeTc(defaultIfNull(result.errorMessage(), "Error")),
                                            escapeTc(defaultIfNull(result.errorDetails(), ""))));
                            break;
                        case SKIPPED:
                            processHandler.log(
                                    String.format("##teamcity[testIgnored name='%s' message='%s']",
                                            escapedName,
                                            escapeTc(defaultIfNull(result.skippedMessage(), "Skipped"))));
                            break;
                        default:
                            break;
                    }

                    processHandler.log(
                            String.format("##teamcity[testFinished name='%s' duration='%s']",
                                    escapedName,
                                    result.timeInMillisStr()));
                });
    }

    private String escapeTc(String s) {
        return s.replace("|", "||")
                .replace("'", "|'")
                .replace("\n", "|n")
                .replace("\r", "|r")
                .replace("[", "|[")
                .replace("]", "|]");
    }

    private String defaultIfNull(String value, String fallback) {
        return value != null ? value : fallback;
    }
}
