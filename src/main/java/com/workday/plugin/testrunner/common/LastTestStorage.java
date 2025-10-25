package com.workday.plugin.testrunner.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the last executed test parameters.
 */
public class LastTestStorage {

    private static boolean isStored = false;

    public static boolean isLastTestStored() {
        return isStored;
    }

    // ====== Core storage fields ======
    private static String runTabName;
    private static String host;
    private static String[] jmxParameters;
    private static Environment environment;
    private static String basePath;

    // ====== Per-tab storage map ======
    private static final Map<String, LastTestEntry> entriesByTabKey =
        Collections.synchronizedMap(new HashMap<>());

    enum Environment {
        LOCAL,
        REMOTE
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
        } else {
            setLocal();
        }
        setBasePath(Locations.getBasePath());
        isStored = true;
            entriesByTabKey.put(runTabName, getLastTestEntry());
    }

    public static boolean isRemote() {
        return environment == Environment.REMOTE;
    }

    public static void setBasePath(final String basePath) {
        LastTestStorage.basePath = basePath;
    }

    public static void setRunTabName(final String runTabName) {
        LastTestStorage.runTabName = runTabName;
    }

    public static String getHost() {
        return host;
    }

    public static void setHost(String host) {
        LastTestStorage.host = host;
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

    // ====== Unified object API ======

    /** Immutable snapshot of the last test state (including tabKey). */
    public static class LastTestEntry {

        private final String host;
        private final boolean isRemote;
        private final String runTabName;
        private final String[] jmxParameters;
        private final String basePath;

        private LastTestEntry(String host, boolean isRemote,
                              String runTabName, String[] jmxParameters, String basePath) {
            this.host = host;
            this.isRemote = isRemote;
            this.runTabName = runTabName;
            this.jmxParameters = jmxParameters;
            this.basePath = basePath;
        }

        public String getHost() {
            return host;
        }

        public boolean isRemote() {
            return isRemote;
        }

        public String getRunTabName() {
            return runTabName;
        }

        public String[] getJmxParameters() {
            return jmxParameters;
        }

        public String getBasePath() {
            return basePath;
        }
    }

    /**
     * Creates a snapshot of the current stored test parameters.
     */
    public static LastTestEntry getLastTestEntry() {
        if (!isStored) return null;
        return new LastTestEntry(
            host,
            isRemote(),
            runTabName,
            jmxParameters,
            basePath
        );
    }

    // ====== API for multi-tab storage ======

    /**
     * Returns a stored LastTestEntry by tabKey.
     * @param tabKey the unique tab key
     * @return the corresponding LastTestEntry or null if not found
     */
    public static LastTestEntry getLastEntry(String tabKey) {
        if (tabKey == null) return null;
        return entriesByTabKey.get(tabKey);
    }
}