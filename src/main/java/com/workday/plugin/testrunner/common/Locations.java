package com.workday.plugin.testrunner.common;

import org.jetbrains.annotations.NotNull;

/**
 * Constants used across the test runner plugin.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class Locations {

    public static final String TEST_RESULTS_FOLDER_SUV_DOCKER = "/usr/local/workday-oms/logs";
    public static final String TEST_RESULTS_FOLDER_SUV = "/data/workdaydevqa/suv/suvots/logs";
    public static final String TEST_RESULTS_FILE = "TEST-junit-jupiter.xml";
    public static final String TEST_JUNIT_JUPITER_XML = "TEST-junit-jupiter.xml";
    public static final String LOCALHOST = "localhost";
    public static final String SUV_RESULTS_FILE = TEST_RESULTS_FOLDER_SUV + "/" + TEST_RESULTS_FILE;

    private static String basePath;

    public static void setBasePath(String basePath) {
        Locations.basePath = basePath;
    }

    public static String getBasePath() {
        return basePath;
    }

    public static @NotNull String getLocalResultFile() {
        return getBasePath() + "/" + TEST_RESULTS_FILE;
    }

}
