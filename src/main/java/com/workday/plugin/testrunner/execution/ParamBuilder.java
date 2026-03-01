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

    /**
     * Converts JMX-style args to a Gradle --tests argument.
     * JMX method format: "com.example.MyClass@myMethod" -> "com.example.MyClass.myMethod"
     * JMX class format:  args[0]="empty", args[1]="com.example.MyClass" -> "com.example.MyClass"
     */
    public static @NotNull String getGradleTestArg(final String @NotNull [] jmxArgs) {
        // Class run: args[0] is "empty", class name is in args[1]
        if ("empty".equals(jmxArgs[0])) {
            return jmxArgs[1];
        }
        // Method run: args[0] is "com.example.MyClass@myMethod"
        return jmxArgs[0].replace("@", ".");
    }

}
