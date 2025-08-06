package com.workday.plugin.testrunner.execution;

import com.intellij.execution.process.ProcessOutputType;

import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * LocalStrategy is a concrete implementation of RunStrategy for execution on local OMS.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */

public class LocalRunStrategy
    implements RunStrategy {

    private final OSCommands osCommands;
    private final String localResultFile;
    private final String testResultsFolderLocal;
    private UiContentDescriptor.UiProcessHandler processHandler;

    public LocalRunStrategy(OSCommands osCommands, String localResultFile, final String testResultsFolderLocal) {
        this.osCommands = osCommands;
        this.localResultFile = localResultFile;
        this.testResultsFolderLocal = testResultsFolderLocal;
    }

    @Override
    public String getJmxResultFolder() {
        return testResultsFolderLocal;
    }

    @Override
    public int getOmsJmxPort() {
        return osCommands.getLocalOmsJmxPort();
    }

    @Override
    public void deleteTempFiles() {
        osCommands.deleteLocalFile(localResultFile);
    }

    @Override
    public void verifyOms() {
        String curlCmd = "http://localhost:12001/ots/-/tenantoperation/-list";
        String output = osCommands.executeLocalCommand("curl " + curlCmd);
        if (!output.contains("\noms: Ready")) {
            final String error = "Error: Installation does not support oms tenant, output = ";
            log(error);
            throw new RuntimeException(error + output);
        }
    }

    private void log(final String error) {
        this.processHandler.notifyTextAvailable(error, ProcessOutputType.STDOUT);
    }

    @Override
    public void copyTestResults() {
        // no-op for local
    }

    @Override
    public void maybeStartPortForwarding(final int jmxPort) {
        // no-op for local
    }

    @Override
    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
        this.osCommands.setProcessHandler(processHandler);
    }
}
