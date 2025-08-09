package com.workday.plugin.testrunner.execution;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

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
                final String result = runTestOms(jmxParams);
                log("Result: " + result + "\n");
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

    private String runTestOms(final String @NotNull [] jmxParams)
        throws IOException, MalformedObjectNameException {
        strategy.maybeStartPortForwarding(jmxPort);
        String hostPort = String.format("localhost:%d", jmxPort);
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + hostPort + "/jmxrmi");
        try (JMXConnector connector = JMXConnectorFactory.connect(url, null)) {
            JUnitTestingMXBean bean = getJUnitTestingMXBean(connector);
            return runCommand(bean, jmxParams);
        }
    }

    private static JUnitTestingMXBean getJUnitTestingMXBean(final JMXConnector jmxConnector)
        throws IOException, MalformedObjectNameException {
        final MBeanServerConnection mbeanConn = jmxConnector.getMBeanServerConnection();
        final ObjectName mbeanName = new ObjectName("com.workday.oms:name=JunitTestListener");
        return JMX.newMBeanProxy(mbeanConn, mbeanName, JUnitTestingMXBean.class);
    }

    private String runCommand(final JUnitTestingMXBean mxBean, final String[] args) {
        log("Running tests with parameters: " + String.join(", ", jmxParams) + ", " + strategy.getJmxResultFolder());
        String result = mxBean.executeTestSuite(args[0], args[1], args[2], args[3], args[4],
            strategy.getJmxResultFolder());
        System.out.println(result);
        return result;
    }

    public interface JUnitTestingMXBean {

        String executeTestSuite(String testMethod,
                                String testClass,
                                String testPackage,
                                String testConcurrent,
                                String testCategory,
                                String toDir);
    }
}

