package com.workday.plugin.testrunner.execution;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * OSCommands class provides methods to execute various OS commands
 * for managing files and processes on local and remote servers.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class OSCommands {
    private static final String SUV_USER = "root";
    private static final String CMD_DELETE_FILE = "rm -f %s";
    private static final String CMD_SCP = "scp -p %s@%s:%s %s"; // -p preserves remote mtime for timestamp checks
    // OTS (local) JMX port discovery
    private static final String CMD_GREP_JMX_PORT = "ps -ef | grep wd.service.type=ots | grep -o 'com.sun.management.jmxremote.port=[0-9]*' | cut -d'=' -f2";
    private static final String CMD_ON_SUV = "ssh -o StrictHostKeyChecking=no -o RequestTTY=no %s@%s %s";
    private static final String CMD_START_PORT_FORWARDING = "ssh -o StrictHostKeyChecking=no -L %d:localhost:%d %s@%s";

    // ORS (remote) JMX port is fixed - defined in ors2-17-17:/usr/local/workday-oms/tomcat/conf/catalina.properties
    private static final int REMOTE_ORS_JMX_PORT = 12096;

    private final String host;
    private UiContentDescriptor.UiProcessHandler processHandler;

    public OSCommands(final String host) {
        this.host = host;
    }

    public void deleteLocalFile(final String file) {
        final String cmd = String.format(CMD_DELETE_FILE, file);
        log(cmd);
        executeLocalCommand(cmd);
    }

    private void log(final String cmd) {
        this.processHandler.log(cmd);
    }

    public void deleteRemoteFile(final String file) {
        executeBestEffort(String.format(CMD_ON_SUV, SUV_USER, host, String.format(CMD_DELETE_FILE, file)));
    }

    public void deleteRemoteDir(final String dir) {
        executeBestEffort(String.format(CMD_ON_SUV, SUV_USER, host, "rm -rf " + dir));
    }

    public void copyFileFromRemote(final String fromFile, final String toFile) {
        executeLocalCommand(String.format(CMD_SCP, SUV_USER, host, fromFile, toFile));
    }

    public int getLocalOmsJmxPort() {
        return parsePort(executeLocalCommand(CMD_GREP_JMX_PORT));
    }

    public int getRemoteOmsJmxPort() {
        log("Using fixed ORS JMX port: " + REMOTE_ORS_JMX_PORT);
        return REMOTE_ORS_JMX_PORT;
    }

    public void startPortForwarding(final int port) {
        final String cmd = String.format(CMD_START_PORT_FORWARDING, port, port, SUV_USER, host);
        try {
            final String[] command = { "/bin/bash", "-c", cmd };
            final Process process = new ProcessBuilder(command).start();
            Runtime.getRuntime().addShutdownHook(new Thread(process::destroy));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Welcome to console login for your Workday SUV Virtual Machine!")) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error: Failed to forward port", e);
        }
    }

    public String executeLocalCommand(final String cmd) {
        return execute(cmd);
    }

    public String executeRemoteCommand(final String cmd) {
        final String remoteCmd = String.format(CMD_ON_SUV, SUV_USER, host, cmd);
        log(remoteCmd);
        return execute(remoteCmd);
    }

    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    private String execute(final String cmd) {
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        try {
            // Source the user's zsh profile to get the full PATH (including cloudflared,
            // which is needed for SSH ProxyCommand on *.prd.workdaysuv.com hosts).
            // IntelliJ runs as a macOS GUI app and does not inherit the terminal PATH.
            final String fullCmd = "source /etc/profile 2>/dev/null; source ~/.zshrc 2>/dev/null; source ~/.zprofile 2>/dev/null; " + cmd;
            ProcessBuilder pb = new ProcessBuilder("/bin/zsh", "-c", fullCmd);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                reader.lines().forEach(line -> output.append(line).append("\n"));
                errorReader.lines().forEach(line -> errorOutput.append(line).append("\n"));
            }
            if (process.waitFor() != 0) {
                throw new RuntimeException("Non-zero exit code\nError: " + errorOutput);
            }
        } catch (Exception e) {
            throw new RuntimeException("OS Command execution failed: " + cmd, e);
        }
        return output.toString();
    }

    /**
     * Like execute(), but does not throw on non-zero exit code.
     * Used for cleanup commands like rm -f where failure is acceptable
     * (e.g. file doesn't exist, or cloudflared noise on .prd. SUV hosts).
     */
    private void executeBestEffort(final String cmd) {
        log(cmd);
        try {
            final String fullCmd = "source /etc/profile 2>/dev/null; source ~/.zshrc 2>/dev/null; source ~/.zprofile 2>/dev/null; " + cmd;
            Process process = new ProcessBuilder("/bin/zsh", "-c", fullCmd).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                reader.lines().forEach(line -> log(line));
                errorReader.lines().forEach(line -> log("(stderr) " + line));
            }
            process.waitFor();
        }
        catch (Exception e) {
            log("Warning: command failed (ignored): " + cmd + " — " + e.getMessage());
        }
    }

    private int parsePort(String output) {
        try {
            return Integer.parseInt(output.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Error parsing port from output: " + output, e);
        }
    }
}