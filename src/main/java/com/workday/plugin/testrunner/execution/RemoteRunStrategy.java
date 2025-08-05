package com.workday.plugin.testrunner.execution;

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

    public RemoteRunStrategy(OSCommands osCommands, String host, String localResultFile, String remotePath, String jmxPath) {
        this.osCommands = osCommands;
        this.host = host;
        this.localResultFile = localResultFile;
        this.remotePath = remotePath;
        this.jmxPath = jmxPath;
    }

    @Override
    public String getJmxResultFolder() {
        return jmxPath;
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
            throw new RuntimeException("Error: Installation does not support oms tenant, output = " + output);
        }
    }

    @Override
    public void maybeStartPortForwarding(final int jmxPort) {
        osCommands.startPortForwarding(jmxPort);
    }
}
