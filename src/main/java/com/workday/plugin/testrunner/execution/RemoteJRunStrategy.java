package com.workday.plugin.testrunner.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.jetbrains.annotations.NotNull;

import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.common.SshProbe;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * RemoteJRunStrategy runs ORS tests via ./gradlew with an injected Groovy init script
 * (tc-listener.gradle) that emits minimal ##OMS| structured events to stdout.
 * OmsEventParser converts these into ##teamcity[ service messages for IntelliJ's test tree.
 *
 * @author alexander.aizikivsky
 * @since Feb-2026
 */
public class RemoteJRunStrategy implements RunStrategy {

    private static final String GRADLE_TASK = ":oms-application:remoteServerTest";
    private static final String GRADLE_EXTRA_PARAMS =
            "-Pjunit.platform.engine.distributor.server.path=ors/execute_remote_junit " +
                    "-Pjunit.platform.engine.distributor.server.port=12701 " +
                    "--rerun";

    private UiContentDescriptor.UiProcessHandler processHandler;

    @Override public String getJmxResultFolder() { return ""; }
    @Override public String getHost() { return Locations.LOCALHOST; }
    @Override public int getOmsJmxPort() { return -1; }
    @Override public void deleteTempFiles() {}
    @Override public void copyTestResults() {}
    @Override public void verifyOms() {}
    @Override public void maybeStartPortForwarding(final int jmxPort) {}
    @Override public boolean bypassJmxProxy() { return false; }
    public void setProject(final com.intellij.openapi.project.Project project) {}

    @Override
    public SshProbe.@NotNull Result getProbe(final String host) {
        return new SshProbe.Result(true, "", 0, "", Locations.LOCALHOST);
    }

    @Override
    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
    }

    public void runGradleTest(final String gradleTestArg) {
        final String basePath = Locations.getBasePath();

        processHandler.log("Terminal command: cd " + basePath + " && ./gradlew " + GRADLE_TASK
                + " --tests \"" + gradleTestArg + "\" " + GRADLE_EXTRA_PARAMS);

        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
            File initScript = null;
            try {
                initScript = extractInitScript();

                final String cmd = "./gradlew " + GRADLE_TASK
                        + " --tests \"" + gradleTestArg + "\" "
                        + GRADLE_EXTRA_PARAMS
                        + " --init-script " + initScript.getAbsolutePath();

                Process process = new ProcessBuilder("/bin/zsh", "-c", cmd)
                        .directory(new File(basePath))
                        .redirectErrorStream(true)
                        .start();

                final OmsEventParser parser = new OmsEventParser(processHandler);

                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!parser.process(line)) {
                            // Not an ##OMS| event — show as regular console output
                            processHandler.log(line);
                        }
                    }
                }

                parser.finish();
                int exitCode = process.waitFor();
                processHandler.finish(exitCode);

            } catch (Exception e) {
                processHandler.error("Failed to run Gradle: " + e.getMessage());
                processHandler.finish(1);
            } finally {
                if (initScript != null) initScript.delete();
            }
        });
    }

    private File extractInitScript() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/tc-listener.gradle")) {
            if (is == null) {
                throw new IllegalStateException("tc-listener.gradle not found in plugin resources");
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            File tmp = File.createTempFile("oms-tc-listener", ".gradle");
            try (FileWriter fw = new FileWriter(tmp)) {
                fw.write(content);
            }
            return tmp;
        }
    }
}