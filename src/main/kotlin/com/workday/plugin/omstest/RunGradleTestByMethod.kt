package com.workday.plugin.omstest

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

/**
 * Action to run a Gradle test for the selected Java method in IntelliJ IDEA.
 * Identifies the method and its containing class at the caret position, then executes the test using Gradle.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunGradleTestByMethod : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val editor: Editor = event.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile: PsiFile = event.getData(CommonDataKeys.PSI_FILE) ?: return

        val offset = editor.caretModel.offset
        val element: PsiElement = psiFile.findElementAt(offset) ?: return

        val method: PsiMethod = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) ?: return
        val clazz: PsiClass = PsiTreeUtil.getParentOfType(element, PsiClass::class.java) ?: return

        val qualifiedName = clazz.qualifiedName ?: return
        val testMethodParam = "$qualifiedName@${method.name}"
        val commandParts = listOf("./gradlew", "-PtestMethod=$testMethodParam", ":runTestJmx", "-s")
        ConsoleRunner.runCommand(commandParts, event.project, "Test: $testMethodParam")
    }

    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        if (editor == null || psiFile == null) {
            event.presentation.isEnabled = false
            return
        }
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        if (element == null) {
            event.presentation.isEnabled = false
            return
        }
        val method = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        event.presentation.isEnabled = method != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}