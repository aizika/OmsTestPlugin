package com.workday.plugin.omstest.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtil

data class MethodTarget(
    val fqName: String,
    val runTabName: String
)

data class ClassTarget(
    val fqName: String,
    val category: String,
    val runTabName: String
)

data class ResolvedContext(
    val project: Project,
    val editor: Editor?,
    val file: PsiFile?,
    val element: PsiElement
)

object ErrorHandlingContext {
    private val quietFlag = ThreadLocal.withInitial { false }

    fun setQuiet() {
        quietFlag.set(true)
    }

    fun isQuiet(): Boolean = quietFlag.get()
}

object TargetResolver {
    fun resolveMethodTargetQuietly(element: PsiElement): MethodTarget? {
        ErrorHandlingContext.setQuiet()
        return resolveMethodTargetCommon(element)
    }

    fun resolveMethodTarget(e: AnActionEvent): MethodTarget? {
        val context = TargetResolverUtil.resolveCommonContext(e) ?: return null
        return resolveMethodTargetCommon(context.element);
    }

    private fun resolveMethodTargetCommon(element: PsiElement): MethodTarget? {
        val method = when (element) {
            is PsiMethod -> element
            is PsiIdentifier -> element.parent as? PsiMethod
            else -> PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        } ?: return null

        val project = element.project

        if (element != method.nameIdentifier) return null

        // Optional: only annotate @Test methods
        val isTestMethod = method.annotations.any {
            it.qualifiedName == "org.junit.Test" || it.qualifiedName == "org.junit.jupiter.api.Test"
        }
        if (!isTestMethod) return null

        val psiClass = PsiUtil.getTopLevelClass(method) ?: method.containingClass ?: return null
        val qualifiedName = psiClass.qualifiedName ?: return null

        val fqMethodName = "$qualifiedName@${method.name}"

        return MethodTarget(fqMethodName, method.name)
    }


    fun resolveClassTargetQuietly(psiElement: PsiElement): ClassTarget? {
        ErrorHandlingContext.setQuiet()
        val psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java) ?: return null
        return resolveClassTargetCommon(psiClass)
    }

    fun resolveClassTarget(e: AnActionEvent): ClassTarget? {
        val context = TargetResolverUtil.resolveCommonContext(e) ?: return null

        val psiClass = PsiTreeUtil.getParentOfType(context.element, PsiClass::class.java)
            ?: TargetResolverUtil.findContainingClass(context)
            ?: return TargetResolverUtil.showError(context.project, "No test class found at cursor.")
        return resolveClassTargetCommon(psiClass)
    }

    private fun resolveClassTargetCommon(psiClass: PsiClass): ClassTarget? {
        val qualifiedName = TargetResolverUtil.qualifiedNameOrError(psiClass) ?: return null

        val category = TargetResolverUtil.getTestCategory(psiClass)
            .takeIf { it.isNotEmpty() }
            ?: return TargetResolverUtil.showError(psiClass.project, "Missing or malformed @Tag annotation.")

        val label = psiClass.name ?: "Run Test"

        return ClassTarget(qualifiedName, category, label)
    }

}

object TargetResolverUtil {
    fun findContainingClass(context: ResolvedContext): PsiClass? {
        return PsiUtil.getTopLevelClass(context.element)
            ?: (context.file as? PsiJavaFile)?.classes?.firstOrNull()
    }

    fun qualifiedNameOrError(psiClass: PsiClass): String? {
        return psiClass.qualifiedName
            ?: showError(psiClass.project, "Cannot determine qualified class name.")
    }

    fun showError(project: Project, message: String, quiet: Boolean = false): Nothing? {
        if (!ErrorHandlingContext.isQuiet()) {
            Messages.showErrorDialog(project, message, "Test Resolver Error")
        }
        return null
    }

    fun resolveCommonContext(e: AnActionEvent): ResolvedContext? {
        val project = e.project ?: return null

        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)

        val elementFromEditor = editor?.let {
            file?.findElementAt(it.caretModel.offset)
        }

        val elementFromSelection = e.getData(CommonDataKeys.PSI_ELEMENT)

        val element = elementFromEditor ?: elementFromSelection
        ?: return showError(project, "No code element found at cursor or selection.")

        return ResolvedContext(project, editor, file, element)
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