package com.workday.plugin.testrunner.target;

/**
 * Represents a test target with its associated metadata.
 * This class encapsulates the details of a test method, class, or package,
 * including the package name, class name, method name, test category, and whether
 * it is a test method.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class TestTarget {

    public final String packageName;
    public final String className;
    public final String methodName;
    public final String testCategory;
    public final boolean isTestMethod;

    public TestTarget(String methodName, String className, String packageName, String testCategory, boolean isTestMethod) {
        this.packageName = packageName;
        this.className = className;
        this.methodName = methodName;
        this.testCategory = testCategory;
        this.isTestMethod = isTestMethod;
    }
}
