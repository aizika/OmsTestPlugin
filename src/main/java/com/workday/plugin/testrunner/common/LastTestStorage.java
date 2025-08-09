package com.workday.plugin.testrunner.common;

/** * Storage for the last test run parameters.
 * This class holds the last run tab name, host, JMX parameters, and whether the run is remote.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class LastTestStorage {

    private static String runTabName;
    private static String host;
    private static String[] jmxParameters;
    private static boolean isRemote;

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

    public static void setIsRemote(final boolean isRemote) {
        LastTestStorage.isRemote = isRemote;
    }

    public static String[] getJmxParameters() {
        return jmxParameters;
    }

    public static boolean getIsRemote() {
        return isRemote;
    }

    public static void setBasePath(final String basePath) {
        LastTestStorage.basePath = basePath;
    }
}