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

    private volatile Process runningProcess;
    private volatile boolean cancelled;

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
                "open localhost:" + this.jmxPort + "\n",
                "domain com.workday.oms\n",
                "bean name=JunitTestListener\n",
                "run executeTestSuite " + jmxparams + "\n").replace("\n", "\\n");

        String sshCommand = buildSshCommand(jmxInput);
        this.handler.log("Running SSH command: \n" + sshCommand);
        runRemoteCommand(sshCommand, "Running test on " + strategy.getHost());
    }

    public void runTestOrs(String[] jmxParams) {
        final String jmxparams = join(" ", jmxParams) + " " + strategy.getJmxResultFolder();
        String jmxInput = "open localhost:" + this.jmxPort + "\\n" +
                          "domain com.workday.oms\\n" +
                          "bean name=JunitTestListener\\n" +
                          "run executeTestSuite " + jmxparams + "\\n" +
                          "exit";
        String sshCommand = buildOrsSshCommand(jmxInput);
        this.handler.log("Running ORS SSH command:\n" + sshCommand);
        runRemoteCommand(sshCommand, "Running test on ORS " + strategy.getHost());
    }

    private String buildOrsSshCommand(String jmxInput) {
        return String.format(
                "ssh -o StrictHostKeyChecking=accept-new root@%s " +
                "\"OMS_PID=\\$(ss -tlnp | grep 12096 | grep -oP 'pid=\\\\K[0-9]+'); " +
                "rm -rf /proc/\\$OMS_PID/root/tmp/testout; " +
                "mkdir -p /proc/\\$OMS_PID/root/tmp/testout; " +
                "chmod 777 /proc/\\$OMS_PID/root/tmp/testout; " +
                "chown 500:500 /proc/\\$OMS_PID/root/tmp/testout; " +
                "echo -e \\\"%s\\\" | java -jar /usr/local/bin/jmxterm-1.0-SNAPSHOT-uber.jar; " +
                "rm -rf /root/testout; cp -r /proc/\\$OMS_PID/root/tmp/testout /root/testout\"",
                strategy.getHost(), jmxInput);
    }

    private String buildSshCommand(String jmxInput) {
        // Note: escape the inner quotes for the remote shell
        return String.format(
                "ssh -o StrictHostKeyChecking=accept-new root@%s " +
                        "\"docker exec ors2-17-17 mkdir -p /usr/local/workday-oms/logs/junit && " +
                        "echo -e \\\"%s\\\" | java -jar /usr/local/bin/jmxterm-1.0-SNAPSHOT-uber.jar\"",
                strategy.getHost(), jmxInput);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Kills the running SSH process and sends cancelRunningTest to OMS via a separate SSH connection
     * to release the "another test is running" lock.
     */
    public void cancel() {
        cancelled = true;
        final Process p = runningProcess;
        if (p != null) {
            p.destroyForcibly();
        }
        final Thread t = new Thread(this::sendCancelToOms, "oms-cancel");
        t.setDaemon(true);
        t.start();
    }

    private void sendCancelToOms() {
        try {
            final String jmxInput = "open localhost:" + jmxPort + "\\n" +
                                    "domain com.workday.oms\\n" +
                                    "bean name=JunitTestListener\\n" +
                                    "run cancelRunningTest\\n" +
                                    "exit";
            final String cmd = String.format(
                    "ssh -o StrictHostKeyChecking=accept-new root@%s " +
                    "\"echo -e \\\"%s\\\" | java -jar /usr/local/bin/jmxterm-1.0-SNAPSHOT-uber.jar\"",
                    strategy.getHost(), jmxInput);
            final String fullCmd = "source /etc/profile 2>/dev/null; source ~/.zshrc 2>/dev/null; source ~/.zprofile 2>/dev/null; " + cmd;
            new ProcessBuilder("/bin/zsh", "-c", fullCmd).start().waitFor();
        } catch (Exception ignored) {
            // best-effort: if this fails the OMS lock will remain until the next test run clears it
        }
    }

    private void runRemoteCommand(String command,
                                  @NlsContexts.ProgressText String title) {
        this.handler.log(title);
        try {
            final String fullCmd = "source /etc/profile 2>/dev/null; source ~/.zshrc 2>/dev/null; source ~/.zprofile 2>/dev/null; " + command;
            Process process = new ProcessBuilder("/bin/zsh", "-c", fullCmd)
                    .redirectErrorStream(true)
                    .start();
            runningProcess = process;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (!isNoise(line)) {
                        this.handler.log(line);
                    }
                }
            }

            process.waitFor();
            runningProcess = null;
        }
        catch (Exception e) {
            if (!cancelled) {
                this.handler.error(e.getMessage());
            }
        }
    }

    /**
     * Filters out known jmxterm noise that is not useful to the user:
     * - EndOfFileException stack trace when the pipe closes
     * - "Process exited with code 1" (jmxterm always exits 1 on pipe close)
     */
    private boolean isNoise(final String line) {
        return line.contains("EndOfFileException")
                || line.contains("at org.jline.")
                || line.contains("at org.cyclopsgroup.jmxterm.")
                || line.contains("Exception in thread \"main\"")
                || line.startsWith("Process exited with code");
    }
}