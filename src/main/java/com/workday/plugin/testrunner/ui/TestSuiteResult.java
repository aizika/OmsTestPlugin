package com.workday.plugin.testrunner.ui;

import java.util.List;

public record TestSuiteResult(String name, int tests, int skipped, int failures, int errors, String timeMillisStr,
                       String hostname, String timestamp, String status, List<TestMethodResult> results) {

}
