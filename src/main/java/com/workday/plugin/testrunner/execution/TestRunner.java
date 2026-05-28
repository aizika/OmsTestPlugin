package com.workday.plugin.testrunner.execution;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import com.workday.plugin.testrunner.common.Locations;
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
            processHandler.log(buildTestingHeader(jmxParams));
            runStrategy.verifyOms();
            final int jmxPort = runStrategy.getOmsJmxPort();
            processHandler.log("OMS JMX port: " + jmxPort);
            processHandler.log("Deleting old result files");
            runStrategy.deleteTempFiles();
            new TestRunner(runStrategy, jmxPort, jmxParams).runTests(processHandler);
        }
        catch (Exception ex) {
            processHandler.error("An error occurred: " + ex.getMessage());
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            processHandler.error(sw.toString());
            descriptor.getUiProcessHandler().finish(1);
        }
    }

    public void runTests(final UiContentDescriptor.UiProcessHandler handler) {
        this.handler = handler;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final long runStartedAt = System.currentTimeMillis();
            try {
                //   JmxTestExecutor supposed to work always, but sometimes it fails to create XML log files.
                //  To deal with this, we use a BypassTestExecutor that runs the JMX command directly on the remote server
                // TODO: Investigate and remove this workaround when JMXTestExecutor is fixed
                if (strategy.bypassJmxProxy()) {
                    final BypassTestExecutor ex = new BypassTestExecutor(strategy, jmxPort, handler);
                    handler.setCancelHandler(ex::cancel);
                    if (strategy.isOrsContainer()) {
                        ex.runTestOrs(jmxParams);
                    } else {
                        ex.runTestOms(jmxParams);
                    }
                    handler.setCancelHandler(null);
                    if (ex.isCancelled()) {
                        handler.log("Test cancelled");
                        handler.finish(0);
                        return;
                    }
                }
                else {
                    new JmxTestExecutor(strategy, jmxPort, handler).runTestOms(jmxParams);
                }
                log("Retrieving test output");
                strategy.copyTestResults();

                final File resultFile = new File(Locations.getLocalResultFile());
                if (!resultFile.exists()) {
                    logError("No result file found — the JMX call produced no output");
                    handler.finish(1);
                    return;
                }
                if (resultFile.lastModified() < runStartedAt) {
                    logError("Result file predates this run — stale results discarded (JMX call likely failed silently)");
                    handler.finish(1);
                    return;
                }

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
        this.handler.log(text);
    }

    private static String buildTestingHeader(final String[] p) {
        final String method  = p[0];
        final String clazz   = p[1];
        final String pkg     = p[2];
        final String category = p[4];

        final StringBuilder sb = new StringBuilder("Testing: (");
        if (!"empty".equals(method)) {
            // method param is "com.example.Foo@myMethod" or "com.example.Foo@myMethod(params)"
            final int at = method.indexOf('@');
            if (at > 0) {
                sb.append("class=").append(method, 0, at)
                  .append(", method=").append(method.substring(at + 1));
            } else {
                sb.append("method=").append(method);
            }
        } else if (!"empty".equals(clazz)) {
            sb.append("class=").append(clazz);
        } else if (!"empty".equals(pkg)) {
            sb.append("package=").append(pkg);
        }
        sb.append(")");
        if (!"empty".equals(category)) {
            sb.append(", category=").append(category);
        }
        return sb.toString();
    }
}