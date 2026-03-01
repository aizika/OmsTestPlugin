package com.workday.plugin.testrunner.execution;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.jetbrains.annotations.NotNull;

import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.common.SshProbe;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * RemoteJRunStrategy runs ORS tests locally via Gradle's remoteServerTest task.
 * Unlike JMX-based strategies, it streams Gradle output directly to the console
 * and relies on IntelliJ's native console parsing for clickable links.
 *
 * Command: ./gradlew :oms-application:remoteServerTest --tests "<testArg>"
 *
 * @author alexander.aizikivsky
 * @since Feb-2026
 */
public class RemoteJRunStrategy
    implements RunStrategy {

    private static final String GRADLE_TASK = ":oms-application:remoteServerTest";

    private UiContentDescriptor.UiProcessHandler processHandler;

    @Override
    public String getJmxResultFolder() {
        // Not used — RemoteJ streams output directly, no XML result file
        return "";
    }

    @Override
    public String getHost() {
        return Locations.LOCALHOST;
    }

    @Override
    public int getOmsJmxPort() {
        // Not used — RemoteJ runs via Gradle, not JMX
        return -1;
    }

    @Override
    public void deleteTempFiles() {
        // no-op — no temp files for RemoteJ
    }

    @Override
    public void copyTestResults() {
        // no-op — results are streamed directly to console
    }

    @Override
    public void verifyOms() {
        // no-op — Gradle will fail naturally if ORS is not running
    }

    @Override
    public void maybeStartPortForwarding(final int jmxPort) {
        // no-op — runs locally
    }

    @Override
    public boolean bypassJmxProxy() {
        // Not applicable — RemoteJ does not use JMX at all
        return false;
    }

    @Override
    public SshProbe.@NotNull Result getProbe(final String host) {
        // Always reachable — runs on localhost
        return new SshProbe.Result(true, "", 0, "", Locations.LOCALHOST);
    }

    @Override
    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    /**
     * Runs the Gradle remoteServerTest task for the given test argument and streams
     * stdout/stderr directly to the Run console.
     *
     * @param gradleTestArg fully qualified test in Gradle format: "com.example.MyTest.myMethod"
     *                      or "com.example.MyTest" for a full class run
     */
    public void runGradleTest(final String gradleTestArg) {
        final String basePath = Locations.getBasePath();
        final String command = String.format(
            "./gradlew %s --tests \"%s\"", GRADLE_TASK, gradleTestArg);

        processHandler.log("Running: " + command);
        processHandler.log("Working directory: " + basePath);

        try {
            Process process = new ProcessBuilder("/bin/bash", "-c", command)
                .directory(new java.io.File(basePath))
                .redirectErrorStream(true)
                .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processHandler.log(line);
                }
            }

            int exitCode = process.waitFor();
            processHandler.log("Process exited with code " + exitCode);
            processHandler.finish(exitCode);
        }
        catch (Exception e) {
            processHandler.error("RemoteJ run failed: " + e.getMessage());
            processHandler.finish(1);
        }
    }
}
