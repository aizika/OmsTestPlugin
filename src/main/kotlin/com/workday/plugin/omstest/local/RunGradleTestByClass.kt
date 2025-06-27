package com.workday.plugin.omstest.local

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.util.TargetResolver
import com.workday.plugin.omstest.util.VisibilityManager

/**
 * Action to run a Gradle test for the selected Java class in IntelliJ IDEA.
 * Extracts the class name from the current file and executes the test using Gradle.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunGradleTestByClass : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val target = TargetResolver.resolveClassTarget(event) ?: return

        val targetName = "-PtestClass=${target.fqName}"
        LocalTestExecutor.runCommand(event.project, target.runTabName, targetName)
    }

    /**
     * Updates the action's enabled state based on the current context.
     * Enables the action if a Java class is found at the caret position.
     */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = VisibilityManager.isClassContext(e)
    }

    /**
     * Specifies that this action should be executed in the background thread.
     * This is important for long-running tasks like running tests.
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}