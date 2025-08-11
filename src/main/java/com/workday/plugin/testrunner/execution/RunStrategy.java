package com.workday.plugin.testrunner.execution;

import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * Interface defining the strategy for running tests in different environments.
 * Implementations provide specific behavior for local and remote test execution.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public interface RunStrategy {
    String getJmxResultFolder();
    String getHost();
    int getOmsJmxPort();
    void deleteTempFiles();
    void copyTestResults();
    void verifyOms();
    void maybeStartPortForwarding(final int jmxPort);
    boolean bypassJmxProxy();

    void setProcessHandler(UiContentDescriptor.UiProcessHandler processHandler);
}

