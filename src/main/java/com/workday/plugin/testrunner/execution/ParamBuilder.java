package com.workday.plugin.testrunner.execution;

import org.jetbrains.annotations.NotNull;

import com.workday.plugin.testrunner.common.TargetType;
import com.workday.plugin.testrunner.target.TestTarget;

/**
 * Utility class to build parameters for JMX execution based on the target type.
 * It provides methods to construct parameter arrays for method, class, and package targets.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class ParamBuilder {

    public static String @NotNull [] getJmxParams(
        TargetType requestedType, TestTarget testTarget) {

        String[] args;
        switch (requestedType) {
        case METHOD -> args = getMethodArgs(testTarget.methodName);
        case CLASS -> args = getClassArgs(testTarget.className);
        case PACKAGE -> args = getPackageArgs(testTarget.packageName, testTarget.testCategory);
        default -> throw new IllegalArgumentException("Unsupported target type");
        }
        return args;
    }

    private static String @NotNull [] getPackageArgs(
        final String packageName, final String testCategory) {
        return new String[] { "empty", "empty", packageName, "empty", testCategory };
    }

    public static String @NotNull [] getClassArgs(final String className) {
        return new String[] { "empty", className, "empty", "empty", "empty" };
    }

    public static String @NotNull [] getMethodArgs(final String methodName) {
        return new String[] { methodName, "empty", "empty", "empty", "empty" };
    }

}
