package com.workday.plugin.testrunner.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import org.jetbrains.annotations.NotNull;

import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.common.SshProbe;
import com.workday.plugin.testrunner.ui.TestResultPresenter;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * RemoteJRunStrategy runs ORS tests via ./gradlew and then parses the JUnit XML
 * results that Gradle writes to build/test-results/remoteServerTest/ to build
 * the test tree with individual test methods.
 *
 * @author alexander.aizikivsky
 * @since Feb-2026
 */
public class RemoteJRunStrategy implements RunStrategy {

    private static final String GRADLE_TASK = ":oms-application:remoteServerTest";
    private static final String GRADLE_EXTRA_PARAMS =
            "-Pjunit.platform.engine.distributor.server.path=ors/execute_remote_junit " +
                    "-Pjunit.platform.engine.distributor.server.port=12090 " +
                    "-Pjunit.platform.engine.distributor.reply.port=43096 " +
                    "--rerun";

    private static final String GRADLE_RESULTS_DIR =
            "oms-application/build/test-results/remoteServerTest";

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
            try {
                final String cmd = "./gradlew " + GRADLE_TASK
                        + " --tests \"" + gradleTestArg + "\" "
                        + GRADLE_EXTRA_PARAMS;

                Process process = new ProcessBuilder("/bin/zsh", "-c", cmd)
                        .directory(new File(basePath))
                        .redirectErrorStream(true)
                        .start();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        processHandler.log(line);
                    }
                }

                int exitCode = process.waitFor();

                String resultDir = basePath + "/" + GRADLE_RESULTS_DIR;
                new TestResultPresenter().displayGradleResults(resultDir, processHandler, exitCode);

            } catch (Exception e) {
                processHandler.error("Failed to run Gradle: " + e.getMessage());
                processHandler.finish(1);
            }
        });
    }
}