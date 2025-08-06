package com.workday.plugin.testrunner.execution;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.intellij.execution.process.ProcessOutputTypes;

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
    private static final String CMD_SCP = "scp %s@%s:%s %s";
    private static final String CMD_GREP_JMX_PORT = "ps -ef | grep wd.service.type=ots | grep -o 'com.sun.management.jmxremote.port=[0-9]*' | cut -d'=' -f2";
    private static final String CMD_ON_SUV = "ssh -o StrictHostKeyChecking=no %s@%s %s";
    private static final String CMD_START_PORT_FORWARDING = "ssh -o StrictHostKeyChecking=no -L %d:localhost:%d %s@%s";

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
        this.processHandler.notifyTextAvailable(cmd + "\n", ProcessOutputTypes.STDOUT);
    }

    public void deleteRemoteFile(final String file) {
        executeRemoteCommand(String.format(CMD_DELETE_FILE, file));
    }

    public void copyFileFromRemote(final String fromFile, final String toFile) {
        executeLocalCommand(String.format(CMD_SCP, SUV_USER, host, fromFile, toFile));
    }

    public int getLocalOmsJmxPort() {
        return parsePort(executeLocalCommand(CMD_GREP_JMX_PORT));
    }

    public int getRemoteOmsJmxPort() {
        return parsePort(executeRemoteCommand(CMD_GREP_JMX_PORT));
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
//                    log(line);
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
            String[] command = { "/bin/bash", "-c", cmd };
            Process process = new ProcessBuilder(command).start();
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

    private int parsePort(String output) {
        try {
            return Integer.parseInt(output.trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException("Error parsing port from output: " + output, e);
        }
    }
}


