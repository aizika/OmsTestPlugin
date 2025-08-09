package com.workday.plugin.testrunner.target;

import com.intellij.psi.*;

/**
 * Utility class to extract test target information from the current context in IntelliJ.
 * It identifies whether a class or method is an OMS test and extracts relevant details.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class TestTargetExtractor {

    public static boolean isOmsTestClass(PsiClass clazz) {
        for (PsiAnnotation annotation : clazz.getAnnotations()) {
            if ("org.junit.jupiter.api.Tag".equals(annotation.getQualifiedName())) {
                for (PsiNameValuePair attr : annotation.getParameterList().getAttributes()) {
                    if (attr.getText().contains("OmsTestCategories.")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static boolean isTestLikeMethod(PsiMethod method) {
        if (method == null) return false;

        String[] testAnnotations = {
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.jupiter.api.RepeatedTest",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate",
            "org.junit.Test"
        };

        for (PsiAnnotation annotation : method.getAnnotations()) {
            String qualifiedName = annotation.getQualifiedName();
            for (String testAnnotation : testAnnotations) {
                if (testAnnotation.equals(qualifiedName)) return true;
            }
        }
        return false;
    }

}