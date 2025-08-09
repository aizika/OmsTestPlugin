package com.workday.plugin.testrunner.ui;

import static com.workday.plugin.testrunner.common.Locations.*;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.application.ApplicationManager;

/**
 * Utility class for displaying parsed JUnit test results in the IntelliJ test runner console.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class TestResultPresenter {

    /**
     * Displays parsed JUnit test results from a file in the IntelliJ test runner console.
     * /Users/alexander.aizikivsky/code/oms/TEST-junit-jupiter.xml
     */
    public void displayParsedResults(UiContentDescriptor.UiProcessHandler processHandler) {
        File logFile = new File(getBasePath(), TEST_JUNIT_JUPITER_XML);
        if (!logFile.exists()) {
            processHandler.finish(1);
            return;
        }

        TestSuiteResult suite = new XmlResultParser().parseTestSuite(logFile);
        ApplicationManager.getApplication().invokeLater(() -> {
            if (suite != null) {
                displayTestSuiteResult(suite, processHandler);
            }
            processHandler.finish(1);
        });
    }

    private void displayTestSuiteResult(TestSuiteResult suite, ProcessHandler processHandler) {
        Map<String, List<TestMethodResult>> grouped = suite.results().stream()
            .collect(Collectors.groupingBy(TestMethodResult::className));

        for (Map.Entry<String, List<TestMethodResult>> entry : grouped.entrySet()) {
            String className = entry.getKey();
            List<TestMethodResult> results = entry.getValue();
            String suiteName = className.substring(className.lastIndexOf('.') + 1);
            String location = "java:" + className;

            processHandler.notifyTextAvailable(
                "##teamcity[testSuiteStarted name='" + suiteName + "' locationHint='" + location + "']\n",
                ProcessOutputTypes.STDOUT
                                              );

            Map<String, TestMethodResult> resultMap = results.stream()
                .collect(Collectors.toMap(TestMethodResult::name, r -> r));
            displayResults(resultMap, processHandler);

            processHandler.notifyTextAvailable(
                "##teamcity[testSuiteFinished name='" + suiteName + "']\n",
                ProcessOutputTypes.STDOUT
                                              );
        }
    }

    private void displayResults(Map<String, TestMethodResult> results, ProcessHandler processHandler) {
        results.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .forEach(result -> {
                String escapedName = escapeTc(result.name());
                String strippedBracketsName = result.name().replaceAll("\\(.*?\\)|\\[.*?]", "");
                String location = "java:" + result.className() + "#" + strippedBracketsName;
                processHandler.notifyTextAvailable(
                    "##teamcity[testStarted name='" + escapedName + "' captureStandardOutput='true' locationHint='"
                        + location + "']\n",
                    ProcessOutputTypes.STDOUT);

                switch (result.status()) {
                case FAILED:
                    processHandler.notifyTextAvailable(
                        String.format("##teamcity[testFailed name='%s' message='%s' details='%s']\n",
                            escapedName,
                            escapeTc(defaultIfNull(result.failureMessage(), "Failed")),
                            escapeTc(defaultIfNull(result.failureDetails(), ""))),
                        ProcessOutputTypes.STDOUT
                                                      );
                    break;
                case ERROR:
                    processHandler.notifyTextAvailable(
                        String.format("##teamcity[testFailed name='%s' message='%s' details='%s']\n",
                            escapedName,
                            escapeTc(defaultIfNull(result.errorMessage(), "Error")),
                            escapeTc(defaultIfNull(result.errorDetails(), ""))),
                        ProcessOutputTypes.STDOUT
                                                      );
                    break;
                case SKIPPED:
                    processHandler.notifyTextAvailable(
                        String.format("##teamcity[testIgnored name='%s' message='%s']\n",
                            escapedName,
                            escapeTc(defaultIfNull(result.skippedMessage(), "Skipped"))),
                        ProcessOutputTypes.STDOUT
                                                      );
                    break;
                default:
                    break;
                }

                processHandler.notifyTextAvailable(
                    String.format("##teamcity[testFinished name='%s' duration='%s']\n",
                        escapedName,
                        result.timeInMillisStr()),
                    ProcessOutputTypes.STDOUT
                                                  );
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
