package com.workday.plugin.testrunner.execution;

import static com.workday.plugin.testrunner.common.Locations.ORS_RESULTS_FILE;
import static com.workday.plugin.testrunner.common.Locations.ORS_TESTOUT_JMX;
import static com.workday.plugin.testrunner.common.Locations.ORS_TESTOUT_REMOTE;

import org.jetbrains.annotations.NotNull;

import com.workday.plugin.testrunner.common.SshProbe;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * RunStrategy for in-container ORS execution.
 * Connects directly via SSH to the SUV host and runs jmxterm inside the ORS PID namespace,
 * writing results to /proc/$OMS_PID/root/tmp/testout and copying them to /root/testout for SCP.
 * No port-forwarding is required.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class OrsRunStrategy implements RunStrategy {

    private final OSCommands osCommands;
    private final String host;
    private final String localResultFile;
    private UiContentDescriptor.UiProcessHandler processHandler;

    public OrsRunStrategy(OSCommands osCommands, String host, String localResultFile) {
        this.osCommands = osCommands;
        this.host = host;
        this.localResultFile = localResultFile;
    }

    @Override
    public String getJmxResultFolder() {
        return ORS_TESTOUT_JMX;
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
        osCommands.deleteRemoteDir(ORS_TESTOUT_REMOTE);
        osCommands.deleteLocalFile(localResultFile);
    }

    @Override
    public void copyTestResults() {
        osCommands.copyFileFromRemote(ORS_RESULTS_FILE, localResultFile);
    }

    @Override
    public void verifyOms() {
        // ORS is always reachable once SSH works; no additional verification needed
    }

    @Override
    public void maybeStartPortForwarding(final int jmxPort) {
        // No port forwarding needed for ORS container access
    }

    @Override
    public boolean bypassJmxProxy() {
        return true;
    }

    @Override
    public boolean isOrsContainer() {
        return true;
    }

    @Override
    public SshProbe.@NotNull Result getProbe(final String host) {
        return SshProbe.probe(host);
    }

    @Override
    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
        this.osCommands.setProcessHandler(processHandler);
    }
}
