package com.workday.plugin.testrunner.execution;

import static com.workday.plugin.testrunner.common.Locations.LOCALHOST;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.application.ApplicationManager;
import com.workday.plugin.testrunner.common.SshProbe;
import com.workday.plugin.testrunner.ui.TestResultPresenter;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * LocalStrategy is a concrete implementation of RunStrategy for execution on local OMS.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */

public class LocalRunStrategy
        implements RunStrategy {

    private static final String JMX_CLIENT_PATH = "/tmp/OmsJmxClient.java";
    private static final String JMX_RESULT_DIR  = "/tmp/oms-jmx-testout";

    // Inline JMX client — same approach as run-jmx-test.sh.
    // Uses Java 11+ single-file source launch (no javac needed).
    private static final String JMX_CLIENT_SOURCE =
        "import javax.management.*;\n" +
        "import javax.management.remote.*;\n" +
        "public class OmsJmxClient {\n" +
        "    public static void main(String[] args) throws Exception {\n" +
        "        String port=args[0],method=args[1],clazz=args[2],pkg=args[3],concurrent=args[4],category=args[5],outDir=args[6];\n" +
        "        JMXServiceURL url=new JMXServiceURL(\"service:jmx:rmi:///jndi/rmi://localhost:\"+port+\"/jmxrmi\");\n" +
        "        try(JMXConnector c=JMXConnectorFactory.connect(url,null)){\n" +
        "            MBeanServerConnection conn=c.getMBeanServerConnection();\n" +
        "            ObjectName bean=new ObjectName(\"com.workday.oms:name=JunitTestListener\");\n" +
        "            Object result=conn.invoke(bean,\"executeTestSuite\",\n" +
        "                new Object[]{method,clazz,pkg,concurrent,category,outDir},\n" +
        "                new String[]{\"java.lang.String\",\"java.lang.String\",\"java.lang.String\",\n" +
        "                             \"java.lang.String\",\"java.lang.String\",\"java.lang.String\"});\n" +
        "            System.out.println(result);\n" +
        "        }\n" +
        "    }\n" +
        "}\n";

    private final OSCommands osCommands;
    private final String localResultFile;
    private final String testResultsFolderLocal;
    private UiContentDescriptor.UiProcessHandler processHandler;

    public LocalRunStrategy(OSCommands osCommands, String localResultFile, final String testResultsFolderLocal) {
        this.osCommands = osCommands;
        this.localResultFile = localResultFile;
        this.testResultsFolderLocal = testResultsFolderLocal;
    }

    @Override
    public String getJmxResultFolder() {
        return testResultsFolderLocal;
    }

    @Override
    public String getHost() {
        return LOCALHOST;
    }

    @Override
    public int getOmsJmxPort() {
        return osCommands.getLocalOmsJmxPort();
    }

    @Override
    public void deleteTempFiles() {
        osCommands.deleteLocalFile(localResultFile);
    }

    @Override
    public void verifyOms() {
        String curlCmd = "http://localhost:12001/ots/-/tenantoperation/-list";
        String output = osCommands.executeLocalCommand("curl " + curlCmd);
        if (!output.contains("\noms: Ready")) {
            final String error = "Error: Installation does not support oms tenant, output = ";
            log(error);
            throw new RuntimeException(error + output);
        }
    }

    private void log(final String error) {
        this.processHandler.log(error);
    }

    @Override
    public void copyTestResults() {
        // no-op for local
    }

    @Override
    public void maybeStartPortForwarding(final int jmxPort) {
        // no-op for local
    }

    @Override
    public boolean bypassJmxProxy() {
        return false;
    }

    @Override
    public SshProbe.@NotNull Result getProbe(final String host) {
        return new SshProbe.Result(true, "", 0, "", LOCALHOST);
    }

    @Override
    public void setProcessHandler(final UiContentDescriptor.UiProcessHandler processHandler) {
        this.processHandler = processHandler;
        this.osCommands.setProcessHandler(processHandler);
    }

    /**
     * Runs the test by spawning a lightweight Java subprocess that calls the OMS JMX MBean
     * directly — same approach as run-jmx-test.sh, with no Gradle overhead.
     * Results are written to {@value JMX_RESULT_DIR} and parsed via TestResultPresenter.
     */
    public void runJmxTest(final String[] jmxParams) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                int port = osCommands.getLocalOmsJmxPort();
                processHandler.log("Local OMS JMX port: " + port);

                Files.writeString(Path.of(JMX_CLIENT_PATH), JMX_CLIENT_SOURCE);

                // Use IntelliJ's own JVM — guaranteed Java 17, always on PATH
                final String javaExe = System.getProperty("java.home") + "/bin/java";

                final String cmd = String.format(
                    "rm -rf '%s' && mkdir -p '%s' && '%s' '%s' %d '%s' '%s' '%s' '%s' '%s' '%s'",
                    JMX_RESULT_DIR, JMX_RESULT_DIR,
                    javaExe, JMX_CLIENT_PATH, port,
                    jmxParams[0], jmxParams[1], jmxParams[2],
                    jmxParams[3], jmxParams[4],
                    JMX_RESULT_DIR
                );

                processHandler.log("Terminal command: java OmsJmxClient " + port + " " + String.join(" ", jmxParams));

                Process process = new ProcessBuilder("/bin/zsh", "-c", cmd)
                        .redirectErrorStream(true)
                        .start();

                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        processHandler.log(line);
                    }
                }

                int exitCode = process.waitFor();
                new TestResultPresenter().displayGradleResults(JMX_RESULT_DIR, processHandler, exitCode);

            } catch (Exception e) {
                processHandler.error("Failed to run local JMX: " + e.getMessage());
                processHandler.finish(1);
            }
        });
    }
}
