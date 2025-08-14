package com.workday.plugin.testrunner.execution;

import static java.lang.String.join;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.intellij.openapi.util.NlsContexts;

import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * Bypass test executor.
 * JmxTestExecutor supposed to work always, but sometimes it fails to create XML log files.
 * To deal with this, we use a BypassTestExecutor that runs the JMX command directly on the remote server
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public final class BypassTestExecutor {

    private final RunStrategy strategy;
    private final int jmxPort;
    private final UiContentDescriptor.UiProcessHandler handler;

    public BypassTestExecutor(final RunStrategy strategy,
                              final int jmxPort,
                              final UiContentDescriptor.UiProcessHandler handler) {
        this.strategy = strategy;
        this.jmxPort = jmxPort;
        this.handler = handler;
    }

    public void runTestOms(String[] jmxParams) {
        // Build JMX input (single-line with literal \n sequences)
        final String jmxparams = join(" ", jmxParams) + " " + strategy.getJmxResultFolder();

        String jmxInput = String.join("\n",
            "open localhost:" + this.jmxPort,
            "domain com.workday.oms",
            "bean name=JunitTestListener",
            "run executeTestSuite " + jmxparams).replace("\n", "\\n");

        String sshCommand = buildSshCommand(jmxInput);
        runRemoteCommand(sshCommand, "Running test on " + strategy.getHost());
    }

    private String buildSshCommand(String jmxInput) {
        // Note: escape the inner quotes for the remote shell
        return String.format(
            "ssh -o StrictHostKeyChecking=accept-new root@%s " +
                "\"docker exec ots-17-17 mkdir -p /usr/local/workday-oms/logs/junit && " +
                "echo -e \\\"%s\\\" | java -jar /usr/local/bin/jmxterm-1.0-SNAPSHOT-uber.jar\"",
            strategy.getHost(), jmxInput);
    }

    private  void runRemoteCommand(String command,
                                   @NlsContexts.ProgressText String title) {
        this.handler.log(title);
        try {
            Process process = new ProcessBuilder("/bin/sh", "-c", command)
                .redirectErrorStream(true)
                .start();

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    this.handler.log(line);
                }
            }

            int exitCode = process.waitFor();
            this.handler.log("Process exited with code " + exitCode);
        }
        catch (Exception e) {
            this.handler.error(e.getMessage());
        }
    }
}