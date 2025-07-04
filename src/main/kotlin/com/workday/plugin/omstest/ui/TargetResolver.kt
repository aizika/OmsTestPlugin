package com.workday.plugin.omstest.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

data class TestTarget(
    val fqName: String,
    val category: String,
    val runTabName: String
)

object ErrorHandlingContext {
    private val quietFlag = ThreadLocal.withInitial { false }

    fun isQuiet(): Boolean = quietFlag.get()
}

object TargetResolver {

    fun resolveMethodTarget(event: AnActionEvent): TestTarget? {
        event.project ?: return null
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val file = event.getData(CommonDataKeys.PSI_FILE) ?: return null
        val offset = editor.caretModel.offset

        val element = file.findElementAt(offset) ?: return null
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return null
        val psiClass = method.containingClass ?: return null

        val fqMethodName = "${psiClass.qualifiedName}@${method.name}"
        val category = TargetResolverUtil.getTestCategory(psiClass)
            .takeIf { it.isNotEmpty() }
            ?: return TargetResolverUtil.showError(psiClass.project, "Missing or malformed @Tag annotation.")

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

        val fqClassName = psiClass.qualifiedName ?: return null
        val category = TargetResolverUtil.getTestCategory(psiClass)
            .takeIf { it.isNotEmpty() }
            ?: return TargetResolverUtil.showError(psiClass.project, "Missing or malformed @Tag annotation.")

        return TestTarget(
            fqName = fqClassName,
            category,
            runTabName = psiClass.name ?: "UnnamedClass"
        )
    }

}

object TargetResolverUtil {

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

}