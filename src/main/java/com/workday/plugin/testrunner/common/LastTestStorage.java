package com.workday.plugin.testrunner.common;

/** * Storage for the last test run parameters.
 * This class holds the last run tab name, host, JMX parameters, and whether the run is remote.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class LastTestStorage {

    private static boolean isStored = false;

    public static boolean isLastTestStored() {
        return isStored;
    }

    public static void setLastTestStorage(final String host,
                                          final boolean isRemote,
                                          final String runTabName,
                                          final String[] jmxParameters) {
        if (isRemote) {
            setHost(host);
        }
        setRunTabName(runTabName);
        setJmxParameters(jmxParameters);
        if (isRemote) {
            setRemote();
        }
        else {
            setLocal();
        }
        setBasePath(Locations.getBasePath());
        isStored = true;
    }

    public static boolean isRemote() {
        return environment == Environment.REMOTE;
    }

    enum Environment {
        LOCAL,
        REMOTE
    }

    private static String runTabName;
    private static String host;
    private static String[] jmxParameters;
    private static Environment environment;

    public static String getBasePath() {
        return basePath;
    }

    private static String basePath;

    public static String getRunTabName() {
        return runTabName;
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        LastTestStorage.host = host;
    }

    public static void setRunTabName(final String runTabName) {
        LastTestStorage.runTabName = runTabName;
    }

    public static void setJmxParameters(final String[] jmxParameters) {
        LastTestStorage.jmxParameters = jmxParameters;
    }

    public static void setRemote() {
        LastTestStorage.environment = Environment.REMOTE;
    }

    public static void setLocal() {
        LastTestStorage.environment = Environment.LOCAL;
    }

    public static String[] getJmxParameters() {
        return jmxParameters;
    }

    public static void setBasePath(final String basePath) {
        LastTestStorage.basePath = basePath;
    }
}