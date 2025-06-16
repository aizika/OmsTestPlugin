package com.workday.plugin.omstest

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Action to run a Gradle test for the selected Java class in IntelliJ IDEA.
 * Extracts the class name from the current file and executes the test using Gradle.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunGradleTestByClass : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val psiFile = event.getData(CommonDataKeys.PSI_FILE) as? PsiJavaFile ?: return
        val clazz: PsiClass = psiFile.classes.firstOrNull() ?: return
        val qualifiedName = clazz.qualifiedName ?: return

        val commandParts = listOf("./gradlew", "-PtestClass=$qualifiedName", ":runTestJmx", "-s")
        ConsoleRunner.runCommand(commandParts, event.project, "Test Class: $qualifiedName")
    }

    /**
     * Updates the action's enabled state based on the current context.
     * Enables the action if a Java class is found at the caret position.
     */
    override fun update(event: AnActionEvent) {
        val editor = event.getData(CommonDataKeys.EDITOR)
        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        if (editor == null || psiFile == null) {
            event.presentation.isEnabled = false
            return
        }
        val offset = editor.caretModel.offset
        val element = psiFile.findElementAt(offset)
        val clazz = PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        event.presentation.isEnabled = clazz != null
    }

    /**
     * Specifies that this action should be executed in the background thread.
     * This is important for long-running tasks like running tests.
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}