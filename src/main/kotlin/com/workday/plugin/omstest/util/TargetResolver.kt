package com.workday.plugin.omstest.util

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.*
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

object TargetResolver {

    fun resolveMethodTarget(e: AnActionEvent): MethodTarget? {
        val context = resolveCommonContext(e) ?: return null

        val method = when (val el = context.element) {
            is PsiMethod -> el
            is PsiIdentifier -> el.parent as? PsiMethod
            else -> PsiTreeUtil.getParentOfType(el, PsiMethod::class.java)
        } ?: return showError(context.project, "No test method found at cursor or selection.")

        val psiClass = findContainingClass(context)
            ?: return showError(context.project, "Cannot determine the containing class.")

        val qualifiedName = qualifiedNameOrError(context.project, psiClass) ?: return null

        val fqMethodName = "$qualifiedName@${method.name}"

        return MethodTarget(fqMethodName, method.name)
    }

    fun resolveClassTargetForIcon(e: AnActionEvent): ClassTarget? {
        val context = resolveCommonContext(e) ?: return null

        val psiClass = PsiTreeUtil.getParentOfType(context.element, PsiClass::class.java)
            ?: findContainingClass(context)
            ?: return showError(context.project, "No test class found at cursor.")

        val qualifiedName = qualifiedNameOrError(context.project, psiClass) ?: return null

        val category = getTestCategory(psiClass)
            .takeIf { it.isNotEmpty() }
            ?: return showError(context.project, "Missing or malformed @Tag annotation.")

        val label = psiClass.name ?: "Run Test"

        return ClassTarget(qualifiedName, category, label)
    }

    fun resolveMethodTargetFromElement(element: PsiElement): MethodTarget? {
        val method = when (element) {
            is PsiMethod -> element
            is PsiIdentifier -> element.parent as? PsiMethod
            else -> PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        } ?: return null

        val psiClass = PsiUtil.getTopLevelClass(method) ?: method.containingClass ?: return null
        val qualifiedName = psiClass.qualifiedName ?: return null

        val fqMethodName = "$qualifiedName@${method.name}"
        val label = "${psiClass.name}.${method.name}"

        return MethodTarget(fqMethodName, label)
    }

    fun resolveClassTargetForIcon(psiElement: PsiElement): ClassTarget? {
        val psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
            ?: return null

        val qualifiedName = psiClass.qualifiedName ?: return null

        val category = getTestCategory(psiClass)
            .takeIf { it.isNotEmpty() }
            ?: return null

        val label = psiClass.name ?: "Run Test"

        return ClassTarget(qualifiedName, category, label)
    }

    private fun getTestCategory(psiClass: PsiClass): String {
        return psiClass.annotations
            .firstOrNull { it.qualifiedName == "org.junit.jupiter.api.Tag" }
            ?.findAttributeValue("value")
            ?.text
            ?.removeSurrounding("\"")
            ?.substringAfterLast('.')
            ?: ""
    }

    private fun resolveCommonContext(e: AnActionEvent): ResolvedContext? {
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

    private fun showError(project: Project, message: String): Nothing? {
        Messages.showErrorDialog(project, message, "Test Resolver Error")
        return null
    }

    private fun qualifiedNameOrError(project: Project, psiClass: PsiClass): String? {
        return psiClass.qualifiedName
            ?: showError(project, "Cannot determine qualified class name.")
    }

    private fun findContainingClass(context: ResolvedContext): PsiClass? {
        return PsiUtil.getTopLevelClass(context.element)
            ?: (context.file as? PsiJavaFile)?.classes?.firstOrNull()
    }

    private data class ResolvedContext(
        val project: Project,
        val editor: Editor?,
        val file: PsiFile?,
        val element: PsiElement
    )

}