package com.workday.plugin.testrunner.execution;

import static com.workday.plugin.testrunner.common.Locations.LOCALHOST;

import org.jetbrains.annotations.NotNull;

import com.workday.plugin.testrunner.common.SshProbe;
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
    public String getHost() {
        return LOCALHOST;
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
        this.processHandler.log(error);
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
    public boolean bypassJmxProxy() {
        return false;
    }

    @Override
    public SshProbe.@NotNull Result getProbe(final String host) {
        return new SshProbe.Result(true, "", 0, "", LOCALHOST);
    }

    @Override
    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
        this.osCommands.setProcessHandler(processHandler);
    }
}
