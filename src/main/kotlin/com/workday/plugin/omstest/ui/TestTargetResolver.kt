package com.workday.plugin.omstest.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

/**
 * Utility object to resolve test targets in IntelliJ IDEA based on the current context.
 * It can resolve targets for methods and classes, and checks if the context is suitable for running tests.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
object TestTargetResolver {

    fun resolveMethodTarget(event: AnActionEvent): TestTarget? {
        event.project ?: return null
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return null
        val offset = editor.caretModel.offset

        val element = file.findElementAt(offset) ?: return null
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return null
        val psiClass = method.containingClass ?: return null
        if (!isOmsTestClass(psiClass)) {
            return showError(psiClass.project, "Selected class is not an OMS test class.")
        }
        if (!isOmsTestMethod(method)) {
            return showError(psiClass.project, "Selected method is not a test method.")
        }

        val fqMethodName = "${psiClass.qualifiedName}@${method.name}"
        val category = getTestCategory(psiClass)
            .takeIf { it.isNotEmpty() }
            ?: return showError(psiClass.project, "Missing or malformed @Tag annotation.")

        return TestTarget(
            fqName = fqMethodName,
            category,
            runTabName = method.name
        )
    }

    fun resolveClassTarget(event: AnActionEvent): TestTarget? {
        event.project ?: return null
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return null
        val offset = editor.caretModel.offset

        val element = file.findElementAt(offset) ?: return null
        val psiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return null
        if (!isOmsTestClass(psiClass)) {
            return showError(psiClass.project, "Selected class is not an OMS test class.")
        }

        val fqClassName = psiClass.qualifiedName ?: return null
        val category = getTestCategory(psiClass)
            .takeIf { it.isNotEmpty() }
            ?: return showError(psiClass.project, "Missing or malformed @Tag annotation.")

        return TestTarget(
            fqName = fqClassName,
            category,
            runTabName = psiClass.name ?: "UnnamedClass"
        )
    }

    fun isClassContext(e: AnActionEvent): Boolean {
        val selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (selectedElement is PsiClass) return true

        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile != null && editor != null) {
            val offset = editor.caretModel.offset
            val element = psiFile.findElementAt(offset)
            if (PsiTreeUtil.getParentOfType(element, PsiClass::class.java) != null) return true
        }

        return false
    }

    fun isMethodContext(e: AnActionEvent): Boolean {
        val selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (selectedElement is PsiMethod) return true

        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile != null && editor != null) {
            val offset = editor.caretModel.offset
            val element = psiFile.findElementAt(offset)
            if (PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) != null) return true
        }

        return false
    }

    fun showError(project: Project, message: String): Nothing? {
        if (!ErrorHandlingContext.isQuiet()) {
            Messages.showErrorDialog(project, message, "Test Resolver Error")
        }
        return null
    }

    fun getTestCategory(psiClass: PsiClass): String {
        return psiClass.annotations
            .firstOrNull { it.qualifiedName == "org.junit.jupiter.api.Tag" }
            ?.findAttributeValue("value")
            ?.text
            ?.removeSurrounding("\"")
            ?.substringAfterLast('.')
            ?: ""
    }

    fun isOmsTestClass(clazz: PsiClass): Boolean {
        return clazz.annotations.any {
            it.qualifiedName == "org.junit.jupiter.api.Tag" &&
                    it.parameterList.attributes.any { attr ->
                        attr.text.contains("OmsTestCategories.")
                    }
        }
    }

    fun isOmsTestMethod(method: PsiMethod): Boolean {
        return method.annotations.any {
            it.qualifiedName in setOf(
                "org.junit.jupiter.api.Test",
                "org.junit.jupiter.api.RepeatedTest"
            )
        }
    }

}

data class TestTarget(
    val fqName: String,
    val category: String,
    val runTabName: String
)

object ErrorHandlingContext {
    private val quietFlag = ThreadLocal.withInitial { false }

    fun isQuiet(): Boolean = quietFlag.get()
}
