package com.workday.plugin.testrunner.execution;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import com.workday.plugin.testrunner.common.SshProbe;
import com.workday.plugin.testrunner.ui.TestResultPresenter;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * This class is responsible for running tests using the specified run strategy.
 * It connects to the OMS JMX server, executes the test suite, and handles the results.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class TestRunner {

    private final RunStrategy strategy;
    private final int jmxPort;
    private final String[] jmxParams;
    private UiContentDescriptor.UiProcessHandler handler;

    public TestRunner(final @NotNull RunStrategy runStrategy, final int jmxPort, final String[] jmxParams) {
        this.strategy = runStrategy;
        this.jmxPort = jmxPort;
        this.jmxParams = jmxParams;
    }

    public static void runTest(
        Project project,
        String host,
        String[] jmxParams,
        RunStrategy runStrategy, final UiContentDescriptor descriptor) {

        final UiContentDescriptor.UiProcessHandler processHandler = descriptor.getUiProcessHandler();
        runStrategy.setProcessHandler(processHandler);

        try {
            processHandler.log("Verifying OMS tenant on host: " + host);
            final SshProbe.Result probe = runStrategy.getProbe(host);
            if (probe.exitCode != 0) {
                processHandler.error("Host " + host + " is not reachable: " + probe.reason);
                descriptor.getUiProcessHandler().destroyProcess();
                RunContentManager.getInstance(project)
                    .removeRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
                return;
            }
            runStrategy.verifyOms();

            final int jmxPort = runStrategy.getOmsJmxPort();
            processHandler.log("OMS JMX port: " + jmxPort);
            processHandler.log("Deleting old result files");
            runStrategy.deleteTempFiles();
            new TestRunner(runStrategy, jmxPort, jmxParams).runTests(processHandler);
        }
        catch (Exception ex) {
            processHandler.error("An error occurred: " + ex.getMessage());
            descriptor.getUiProcessHandler().destroyProcess();
            RunContentManager.getInstance(project)
                .removeRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
        }
    }

    public void runTests(final UiContentDescriptor.UiProcessHandler handler) {
        this.handler = handler;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                //   JmxTestExecutor supposed to work always, but sometimes it fails to create XML log files.
                //  To deal with this, we use a BypassTestExecutor that runs the JMX command directly on the remote server
                // TODO: Investigate and remove this workaround when JMXTestExecutor is fixed
                if (strategy.bypassJmxProxy()) {
                    new BypassTestExecutor(strategy, jmxPort, handler).runTestOms(jmxParams);
                }
                else {
                    new JmxTestExecutor(strategy, jmxPort, handler).runTestOms(jmxParams);
                }
                log("Retrieving test output");
                strategy.copyTestResults();
                new TestResultPresenter().displayParsedResults(handler);
            }
            catch (Exception ex) {
                logError("❌ Test run failed: " + ex.getMessage());
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                final String string = sw.toString();
                logError(string);
                handler.finish(1);
            }
        });
    }

    private void logError(final String string) {
        this.handler.error(string);
    }

    private void log(final String text) {
        this.handler.log(text + "\n");
    }
}