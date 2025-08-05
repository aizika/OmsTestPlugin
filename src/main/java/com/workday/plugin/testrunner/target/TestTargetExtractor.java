package com.workday.plugin.testrunner.target;

import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;

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

    public static @NotNull TestTarget getTestTarget(@NotNull AnActionEvent event) {
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiElement fallback = event.getData(CommonDataKeys.PSI_ELEMENT);

        String packageName = getPackage(psiFile, fallback);
        PsiClass psiClass = getPsiClass(psiFile, editor, fallback);
        PsiMethod method = getPsiMethod(psiFile, editor, fallback);

        return new TestTarget(
            buildMethodSignature(getClassName(psiClass), method),
            getClassName(psiClass),
            packageName,
            extractTestCategory(psiClass),
            isTestLikeMethod(method)
        );
    }

    static String getPackage(PsiFile psiFile, PsiElement fallback) {
        if (psiFile instanceof PsiJavaFile javaFile) {
            return javaFile.getPackageName();
        }
        PsiJavaFile fromElement = PsiTreeUtil.getParentOfType(fallback, PsiJavaFile.class, false);
        return (fromElement != null) ? fromElement.getPackageName() : "";
    }

    static PsiClass getPsiClass(PsiFile psiFile, Editor editor, PsiElement fallback) {
        return PsiTreeUtil.getParentOfType(getElement(psiFile, editor, fallback), PsiClass.class);
    }

    static PsiMethod getPsiMethod(PsiFile psiFile, Editor editor, PsiElement fallback) {
        return PsiTreeUtil.getParentOfType(getElement(psiFile, editor, fallback), PsiMethod.class);
    }

    private static PsiElement getElement(PsiFile psiFile, Editor editor, PsiElement fallback) {
        if (psiFile != null && editor != null) {
            PsiElement at = psiFile.findElementAt(editor.getCaretModel().getOffset());
            return (at != null) ? at : fallback;
        }
        return fallback;
    }

    static String extractTestCategory(PsiClass psiClass) {
        if (psiClass == null) return "";

        PsiAnnotation tagAnnotation = getAnnotation(psiClass);
        if (tagAnnotation == null) return "";

        PsiAnnotationMemberValue value = tagAnnotation.findAttributeValue("value");
        if (value instanceof PsiReferenceExpression ref) {
            String text = ref.getText();
            if (text.startsWith("OmsTestCategories.")) {
                String[] parts = text.split("\\.");
                return (parts.length == 2) ? parts[1] : "";
            }
        }
        return "";
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

    public static String buildMethodSignature(String classFqName, PsiMethod method) {
        if (method == null) return classFqName;

        StringBuilder paramTypes = new StringBuilder();
        for (PsiParameter param : method.getParameterList().getParameters()) {
            if (!paramTypes.isEmpty()) paramTypes.append(",");
            paramTypes.append(param.getType().getCanonicalText());
        }

        return classFqName + "@" + method.getName() +
            (paramTypes.isEmpty() ? "" : "(" + paramTypes + ")");
    }

    private static PsiAnnotation getAnnotation(PsiClass psiClass) {
        PsiModifierList modifierList = psiClass.getModifierList();
        return (modifierList != null) ? modifierList.findAnnotation("org.junit.jupiter.api.Tag") : null;
    }

    static String getClassName(PsiClass psiClass) {
        return (psiClass != null) ? psiClass.getQualifiedName() : "";
    }
}