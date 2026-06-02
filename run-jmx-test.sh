#!/bin/zsh
# run-jmx-test.sh — Run an OMS test class via JMX without Gradle or jmxterm.
# Uses the Java JMX API directly, matching what JmxTestExecutor does in the plugin.
#
# Usage:
#   ./run-jmx-test.sh [fully.qualified.TestClass]
#
# Example:
#   ./run-jmx-test.sh com.workday.bi.businessview.BVRollupFieldTest

set -e

TEST_CLASS="${1:-com.workday.bi.alpine.AlpineFilteringTest}"
OUT_DIR="/tmp/oms-jmx-testout"
JAVA_SRC="/tmp/OmsJmxClient.java"

# ---- 1. Discover ORS JMX port ----
JMX_PORT=$(ps -ef | grep 'wd.service.type=' \
    | grep -v grep \
    | grep -o 'com.sun.management.jmxremote.port=[0-9]*' \
    | cut -d'=' -f2 \
    | head -1)

if [[ -z "$JMX_PORT" ]]; then
    echo "ERROR: ORS JMX port not found. Is local ORS running?"
    exit 1
fi
echo "ORS JMX port : $JMX_PORT"
echo "Test class   : $TEST_CLASS"
echo "Output dir   : $OUT_DIR"
echo ""

# ---- 2. Prepare output directory ----
rm -rf "$OUT_DIR" && mkdir -p "$OUT_DIR"

# ---- 3. Write inline JMX client ----
cat > "$JAVA_SRC" << 'EOF'
import javax.management.*;
import javax.management.remote.*;

public class OmsJmxClient {
    public static void main(String[] args) throws Exception {
        String port   = args[0];
        String clazz  = args[1];
        String outDir = args[2];

        JMXServiceURL url = new JMXServiceURL(
            "service:jmx:rmi:///jndi/rmi://localhost:" + port + "/jmxrmi");

        System.out.println("Connecting to localhost:" + port + " ...");
        try (JMXConnector connector = JMXConnectorFactory.connect(url, null)) {
            MBeanServerConnection conn = connector.getMBeanServerConnection();
            ObjectName bean = new ObjectName("com.workday.oms:name=JunitTestListener");
            System.out.println("Invoking executeTestSuite...");
            Object result = conn.invoke(bean, "executeTestSuite",
                new Object[]{ "empty", clazz, "empty", "empty", "empty", outDir },
                new String[]{ "java.lang.String", "java.lang.String", "java.lang.String",
                              "java.lang.String", "java.lang.String", "java.lang.String" });
            System.out.println("MBean response: " + result);
        }
    }
}
EOF

# ---- 4. Run it (Java 11+ source-file launch — no javac needed) ----
java "$JAVA_SRC" "$JMX_PORT" "$TEST_CLASS" "$OUT_DIR"

# ---- 5. Show results ----
echo ""
echo "Result files in $OUT_DIR:"
ls -la "$OUT_DIR" 2>/dev/null || echo "(directory is empty — test may have failed to write XML)"
