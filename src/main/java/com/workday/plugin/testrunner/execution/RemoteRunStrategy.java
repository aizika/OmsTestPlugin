package com.workday.plugin.testrunner.execution;

import org.jetbrains.annotations.NotNull;

import com.workday.plugin.testrunner.common.SshProbe;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * RemoteStrategy class implements the RunStrategy for remote execution on SUV.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class RemoteRunStrategy
    implements RunStrategy {

    private final OSCommands osCommands;
    private final String host;
    private final String localResultFile;
    private final String remotePath;
    private final String jmxPath;
    private UiContentDescriptor.UiProcessHandler processHandler;

    public RemoteRunStrategy(OSCommands osCommands,
                             String host,
                             String localResultFile,
                             String remotePath,
                             String jmxPath) {
        this.osCommands = osCommands;
        this.host = host;
        this.localResultFile = localResultFile;
        this.remotePath = remotePath;
        this.jmxPath = jmxPath;
    }

    public SshProbe.@NotNull Result getProbe(final String host) {
        return SshProbe.probe(host);
    }

    @Override
    public String getJmxResultFolder() {
        return jmxPath;
    }

    @Override
    public String getHost() {
        return this.host;
    }

    @Override
    public int getOmsJmxPort() {
        return osCommands.getRemoteOmsJmxPort();
    }

    @Override
    public void deleteTempFiles() {
        osCommands.deleteLocalFile(localResultFile);
        osCommands.deleteRemoteFile(remotePath);
    }

    @Override
    public void copyTestResults() {
        osCommands.copyFileFromRemote(remotePath, localResultFile);
    }

    @Override
    public void verifyOms() {
        String curlCmd = "https://" + host + "/ots/-/tenantoperation/-list";
        String output = osCommands.executeRemoteCommand("curl " + curlCmd);
        if (!output.contains("\noms: Ready")) {
            final String errorMessage = "Error: Installation does not support oms tenant";
            this.processHandler.error(output);
            this.processHandler.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }
        this.processHandler.log("Oms tenant found.");
    }

    @Override
    public void maybeStartPortForwarding(final int jmxPort) {
        this.processHandler.log("Starting port forwarding for JMX on port " + jmxPort);
        osCommands.startPortForwarding(jmxPort);
    }

    @Override
    public boolean bypassJmxProxy() {
        return true;
    }

    @Override
    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
        this.osCommands.setProcessHandler(processHandler);
    }
}
