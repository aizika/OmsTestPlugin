package com.workday.plugin.testrunner.execution;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

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
            processHandler.notifyTextAvailable("Running test on " + host + "\n", ProcessOutputTypes.STDOUT);

            final int jmxPort = runStrategy.getOmsJmxPort();
            runStrategy.deleteTempFiles();
            runStrategy.verifyOms();
            new TestRunner(runStrategy, jmxPort, jmxParams).runTests(processHandler);
        }
        catch (Exception ex) {
            processHandler.notifyTextAvailable(
                "An error occurred: " + ex.getMessage() + "\n", ProcessOutputTypes.STDERR);
            descriptor.getUiProcessHandler().destroyProcess();
            RunContentManager.getInstance(project)
                .removeRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
        }
    }

    public void runTests(final UiContentDescriptor.UiProcessHandler handler) {
        this.handler = handler;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                log("Running tests");
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
                logError("❌ Test run failed: " + ex.getMessage() + "\n");
                StringWriter sw = new StringWriter();
                ex.printStackTrace(new PrintWriter(sw));
                final String string = sw.toString();
                logError(string);
                handler.finish(1);
            }
        });
    }

    private void logError(final String string) {
        this.handler.notifyTextAvailable(string, ProcessOutputTypes.STDERR);
    }

    private void log(final String text) {
        this.handler.notifyTextAvailable(text + "\n", ProcessOutputTypes.STDOUT);
    }
}