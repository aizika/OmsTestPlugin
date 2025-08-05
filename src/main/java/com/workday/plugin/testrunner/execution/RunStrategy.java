package com.workday.plugin.testrunner.execution;

/**
 * Interface defining the strategy for running tests in different environments.
 * Implementations provide specific behavior for local and remote test execution.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public interface RunStrategy {
    String getJmxResultFolder();
    int getOmsJmxPort();
    void deleteTempFiles();
    void copyTestResults();
    void verifyOms();
    void maybeStartPortForwarding(final int jmxPort);
}

