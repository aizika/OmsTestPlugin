package com.workday.plugin.testrunner.target;

import com.workday.plugin.testrunner.common.TargetType;

/**
 *
 * Utility class to verify the validity of a test target based on its type.
 * It checks for null or empty values and ensures that the target meets the
 * requirements for its specific type (class, method, or package).
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class TargetVerifier {

    public static void verifyTarget(TestTarget target, TargetType type) {
        if (target == null || type == null) {
            throw new IllegalArgumentException("Null target or type");
        }
        switch (type) {
        case CLASS -> verifyClass(target.className, target.testCategory);
        case METHOD -> verifyMethod(target.methodName, target.isTestMethod);
        case PACKAGE -> verifyPackage(target.packageName);
        }
    }

    private static void verifyPackage(final String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("Missing package name");
        }
    }

    private static void verifyMethod(final String methodName, final boolean isTestMethod) {
        if (methodName == null || methodName.isEmpty()) {
            throw new IllegalArgumentException("Missing method name");
        }
        if (!isTestMethod) {
            throw new IllegalArgumentException("Not a test method: " + methodName);
        }
    }

    private static void verifyClass(final String className, final String testCategory) {
        if (className == null || className.isEmpty()) {
            throw new IllegalArgumentException("Missing class name");
        }
        if (testCategory.isBlank()) {
            throw new IllegalArgumentException("Missing test category for class: " + className);
        }
    }
}
