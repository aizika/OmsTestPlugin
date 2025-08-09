package com.workday.plugin.testrunner.execution;

import org.jetbrains.annotations.NotNull;

/**
 * Utility class to build parameters for JMX execution based on the target type.
 * It provides methods to construct parameter arrays for method, class, and package targets.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class ParamBuilder {

    public static String @NotNull [] getClassArgs(final String className) {
        return new String[] { "empty", className, "empty", "empty", "empty" };
    }

    public static String @NotNull [] getMethodArgs(final String methodName) {
        return new String[] { methodName, "empty", "empty", "empty", "empty" };
    }

}
