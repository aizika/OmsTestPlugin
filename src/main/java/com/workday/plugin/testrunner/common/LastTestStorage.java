package com.workday.plugin.testrunner.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intellij.ide.util.PropertiesComponent;

/**
 * Stores the last executed test parameters.
 */
public class LastTestStorage {
//    private static final String KEY_LAST_HOST = "oms.lastHost";
    private static final String KEY_HOST_HISTORY = "oms.hostHistory"; // CSV
    private static final int MAX_HISTORY = 10;

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
        REMOTEJ,
        ORS,
        LOCAL_JMX
    }

    public static void setLastTestStorageLocalJmx(final String runTabName,
                                                   final String[] jmxParameters) {
        setRunTabName(runTabName);
        setJmxParameters(jmxParameters);
        setLocalJmx();
        setBasePath(Locations.getBasePath());
        isStored = true;

        entriesByTabKey.put(runTabName, getLastTestEntry());
    }

    public static void setLastTestStorageOrs(final String host,
                                             final String runTabName,
                                             final String[] jmxParameters) {
        setHost(host);
        setRunTabName(runTabName);
        setJmxParameters(jmxParameters);
        setOrs();
        setBasePath(Locations.getBasePath());
        isStored = true;

        entriesByTabKey.put(runTabName, getLastTestEntry());
    }

    public static void setLastTestStorageRemoteJ(final String runTabName,
                                                 final String[] jmxParameters) {
        setRunTabName(runTabName);
        setJmxParameters(jmxParameters);
        setRemoteJ();
        setBasePath(Locations.getBasePath());
        isStored = true;

        entriesByTabKey.put(runTabName, getLastTestEntry());
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

    public static void setRemoteJ() {
        LastTestStorage.environment = Environment.REMOTEJ;
    }

    public static boolean isRemoteJ() {
        return environment == Environment.REMOTEJ;
    }

    public static void setOrs() {
        LastTestStorage.environment = Environment.ORS;
    }

    public static boolean isOrs() {
        return environment == Environment.ORS;
    }

    public static void setLocalJmx() {
        LastTestStorage.environment = Environment.LOCAL_JMX;
    }

    public static boolean isLocalJmx() {
        return environment == Environment.LOCAL_JMX;
    }

    // ====== Unified object API ======

    /** Immutable snapshot of the last test state (including tabKey). */
    public static class LastTestEntry {

        private final String host;
        private final boolean isRemoteJ;
        private final boolean isOrs;
        private final boolean isLocalJmx;
        private final String runTabName;
        private final String[] jmxParameters;
        private final String basePath;

        private LastTestEntry(String host, boolean isRemoteJ, boolean isOrs, boolean isLocalJmx,
                              String runTabName, String[] jmxParameters, String basePath) {
            this.host = host;
            this.isRemoteJ = isRemoteJ;
            this.isOrs = isOrs;
            this.isLocalJmx = isLocalJmx;
            this.runTabName = runTabName;
            this.jmxParameters = jmxParameters;
            this.basePath = basePath;
        }

        public String getHost() {
            return host;
        }

        public boolean isRemoteJ() {
            return isRemoteJ;
        }

        public boolean isOrs() {
            return isOrs;
        }

        public boolean isLocalJmx() {
            return isLocalJmx;
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
        if (!isStored) {
            return null;
        }
        return new LastTestEntry(
            host,
            isRemoteJ(),
            isOrs(),
            isLocalJmx(),
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
        if (tabKey == null) {
            return null;
        }
        return entriesByTabKey.get(tabKey);
    }

    public static List<String> getRecentHosts() {
        String csv = PropertiesComponent.getInstance().getValue(KEY_HOST_HISTORY, "");
        if (csv.isBlank()) return List.of();
        return new ArrayList<>(Arrays.asList(csv.split("\\s*,\\s*")));
    }

    public static void addRecentHost(String host) {
        ArrayList<String> list = new ArrayList<>(getRecentHosts());
        list.removeIf(h -> h.equalsIgnoreCase(host)); // dedupe
        list.add(0, host);
        if (list.size() > MAX_HISTORY) list.subList(MAX_HISTORY, list.size()).clear();
        String csv = String.join(",", list);
        PropertiesComponent.getInstance().setValue(KEY_HOST_HISTORY, csv);
    }
}