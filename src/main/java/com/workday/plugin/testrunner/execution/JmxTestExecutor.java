package com.workday.plugin.testrunner.execution;

import static java.lang.String.join;
import static java.lang.Thread.sleep;

import java.io.IOException;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.intellij.execution.process.ProcessOutputTypes;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * Extracted local (JMX) test runner.
 * Contains the previous TestRunner#runTestOms and its dependencies with minimal changes.
 */
public class JmxTestExecutor {

    private final RunStrategy strategy;
    private final int jmxPort;
    private final UiContentDescriptor.UiProcessHandler handler;

    public JmxTestExecutor(final RunStrategy strategy,
                           final int jmxPort,
                           final UiContentDescriptor.UiProcessHandler handler) {
        this.strategy = strategy;
        this.jmxPort = jmxPort;
        this.handler = handler;
    }

    public String runTestOms(final String[] jmxParams)
        throws IOException, MalformedObjectNameException {
        strategy.maybeStartPortForwarding(jmxPort);
        try {
            sleep(2000); // wait for port forwarding to be established
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
        log("Running tests with parameters: " + join(", ", args) + ", " + strategy.getJmxResultFolder());
        String result = mxBean.executeTestSuite(args[0], args[1], args[2], args[3], args[4],
            strategy.getJmxResultFolder());
        log(result);
        return result;
    }

    private void log(final String text) {
        this.handler.notifyTextAvailable(text + "\n", ProcessOutputTypes.STDOUT);
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