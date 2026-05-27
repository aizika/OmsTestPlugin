package com.workday.plugin.testrunner.ui;

import static com.workday.plugin.testrunner.common.Locations.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
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
            displayTestResults(suite.results(), processHandler);
            int exitCode = (suite.failures() > 0 || suite.errors() > 0) ? 1 : 0;
            processHandler.finish(exitCode);
        });
    }

    /**
     * Parses all TEST-*.xml files from a Gradle test results directory and renders
     * individual test methods in the test tree. Called by RemoteJRunStrategy after
     * the Gradle process exits.
     */
    public void displayGradleResults(String resultDir, UiContentDescriptor.UiProcessHandler processHandler, int exitCode) {
        File dir = new File(resultDir);
        File[] xmlFiles = dir.listFiles((d, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));

        if (xmlFiles == null || xmlFiles.length == 0) {
            processHandler.log("No test result XML files found in " + resultDir);
            processHandler.finish(exitCode);
            return;
        }

        XmlResultParser parser = new XmlResultParser();
        List<TestMethodResult> allResults = new ArrayList<>();
        boolean anyFailure = false;

        for (File xmlFile : xmlFiles) {
            TestSuiteResult suite = parser.parseTestSuite(xmlFile);
            if (suite != null) {
                allResults.addAll(suite.results());
                if (suite.failures() > 0 || suite.errors() > 0) {
                    anyFailure = true;
                }
            }
        }

        if (allResults.isEmpty()) {
            processHandler.log("Warning: no test results parsed from " + resultDir);
            processHandler.finish(exitCode);
            return;
        }

        final int finalExitCode = anyFailure ? 1 : exitCode;
        ApplicationManager.getApplication().invokeLater(() -> {
            displayTestResults(allResults, processHandler);
            processHandler.finish(finalExitCode);
        });
    }

    /**
     * Renders a 4-level test tree: package → class → method → parameterized variant.
     * Non-parameterized tests appear directly under the class node (3 levels).
     */
    private void displayTestResults(List<TestMethodResult> results, UiContentDescriptor.UiProcessHandler processHandler) {
        Map<String, List<TestMethodResult>> byClass = new TreeMap<>(
                results.stream().collect(Collectors.groupingBy(TestMethodResult::className)));

        // Build package hierarchy.
        // childPkgs: parent pkg → immediate child pkgs; virtual root "" → top-level pkgs.
        // pkgClasses: pkg → class FQNs whose package is exactly pkg.
        TreeMap<String, TreeSet<String>> childPkgs = new TreeMap<>();
        TreeMap<String, TreeSet<String>> pkgClasses = new TreeMap<>();

        for (String className : byClass.keySet()) {
            int dot = className.lastIndexOf('.');
            String pkg = dot >= 0 ? className.substring(0, dot) : "";
            pkgClasses.computeIfAbsent(pkg, k -> new TreeSet<>()).add(className);

            String child = pkg;
            while (!child.isEmpty()) {
                int lastDot = child.lastIndexOf('.');
                String parent = lastDot >= 0 ? child.substring(0, lastDot) : "";
                childPkgs.computeIfAbsent(parent, k -> new TreeSet<>()).add(child);
                childPkgs.computeIfAbsent(child, k -> new TreeSet<>());
                child = parent;
            }
        }

        // Render root packages (children of the virtual root "")
        for (String rootPkg : childPkgs.getOrDefault("", new TreeSet<>())) {
            renderPackageSuite(rootPkg, childPkgs, pkgClasses, byClass, processHandler);
        }
        // Classes in the default (no-name) package
        for (String className : pkgClasses.getOrDefault("", new TreeSet<>())) {
            renderClassSuite(className, byClass.get(className), processHandler);
        }
    }

    private void renderPackageSuite(String pkg,
            TreeMap<String, TreeSet<String>> childPkgs,
            TreeMap<String, TreeSet<String>> pkgClasses,
            Map<String, List<TestMethodResult>> byClass,
            UiContentDescriptor.UiProcessHandler processHandler) {
        processHandler.log("##teamcity[testSuiteStarted name='" + escapeTc(pkg)
                + "' locationHint='pkg:" + pkg + "']");
        for (String subPkg : childPkgs.getOrDefault(pkg, new TreeSet<>())) {
            renderPackageSuite(subPkg, childPkgs, pkgClasses, byClass, processHandler);
        }
        for (String className : pkgClasses.getOrDefault(pkg, new TreeSet<>())) {
            renderClassSuite(className, byClass.get(className), processHandler);
        }
        processHandler.log("##teamcity[testSuiteFinished name='" + escapeTc(pkg) + "']");
    }

    private void renderClassSuite(String className, List<TestMethodResult> results,
            UiContentDescriptor.UiProcessHandler processHandler) {
        String suiteName = className.substring(className.lastIndexOf('.') + 1);

        processHandler.log("##teamcity[testSuiteStarted name='" + escapeTc(suiteName)
                + "' locationHint='java:" + className + "']");

        Map<String, List<TestMethodResult>> byMethod = new TreeMap<>(
                results.stream().collect(Collectors.groupingBy(r -> methodNameOf(r.name()))));

        for (Map.Entry<String, List<TestMethodResult>> methodEntry : byMethod.entrySet()) {
            String methodName = methodEntry.getKey();
            List<TestMethodResult> variants = methodEntry.getValue();
            // JUnit 5 appends "()" to non-parameterized test display names in Gradle XML.
            // Strip trailing "()" before comparing so those tests don't get a spurious suite node.
            String rawName = variants.get(0).name().replaceAll("\\(\\)\\s*$", "").trim();
            boolean isParameterized = variants.size() > 1 || !methodName.equals(rawName);

            if (isParameterized) {
                processHandler.log("##teamcity[testSuiteStarted name='" + escapeTc(methodName)
                        + "' locationHint='java:" + className + "#" + methodName + "']");
                variants.stream()
                        .sorted(Comparator.comparing(TestMethodResult::name))
                        .forEach(v -> displaySingleResult(v, methodName, processHandler));
                processHandler.log("##teamcity[testSuiteFinished name='" + escapeTc(methodName) + "']");
            } else {
                displaySingleResult(variants.get(0), methodName, processHandler);
            }
        }

        processHandler.log("##teamcity[testSuiteFinished name='" + escapeTc(suiteName) + "']");
    }

    private void displaySingleResult(TestMethodResult result, String methodName,
                                     UiContentDescriptor.UiProcessHandler processHandler) {
        String escapedName = escapeTc(result.name());
        String location = "java:" + result.className() + "#" + methodName;

        processHandler.log("##teamcity[testStarted name='" + escapedName
                + "' captureStandardOutput='true' locationHint='" + location + "']");

        switch (result.status()) {
            case FAILED:
                processHandler.log(String.format("##teamcity[testFailed name='%s' message='%s' details='%s']",
                        escapedName,
                        escapeTc(defaultIfNull(result.failureMessage(), "Failed")),
                        escapeTc(defaultIfNull(result.failureDetails(), ""))));
                break;
            case ERROR:
                processHandler.log(String.format("##teamcity[testFailed name='%s' message='%s' details='%s']",
                        escapedName,
                        escapeTc(defaultIfNull(result.errorMessage(), "Error")),
                        escapeTc(defaultIfNull(result.errorDetails(), ""))));
                break;
            case SKIPPED:
                processHandler.log(String.format("##teamcity[testIgnored name='%s' message='%s']",
                        escapedName,
                        escapeTc(defaultIfNull(result.skippedMessage(), "Skipped"))));
                break;
            default:
                break;
        }

        processHandler.log(String.format("##teamcity[testFinished name='%s' duration='%s']",
                escapedName, result.timeInMillisStr()));
    }

    /** Strips parameter types and variant info to get the bare Java method name. */
    static String methodNameOf(String testName) {
        return testName.replaceAll("[\\[(].*", "").trim();
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
