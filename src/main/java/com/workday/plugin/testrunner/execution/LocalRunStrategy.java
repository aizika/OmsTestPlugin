package com.workday.plugin.testrunner.execution;

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
            throw new RuntimeException("Error: Installation does not support oms tenant, output = " + output);
        }
    }

    @Override
    public void copyTestResults() {
        // no-op for local
    }

    @Override
    public void maybeStartPortForwarding(final int jmxPort) {
        // no-op for local
    }
}
