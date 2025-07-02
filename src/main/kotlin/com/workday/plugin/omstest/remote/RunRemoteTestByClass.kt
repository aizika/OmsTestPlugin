package com.workday.plugin.omstest.remote

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.util.TargetResolver
import com.workday.plugin.omstest.util.VisibilityManager

/**
 * Action to run a remote test for the selected Java class in IntelliJ IDEA.
 * Identifies the class at the caret position, then executes the test remotely.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunRemoteTestByClass : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("RunRemoteTestByClass triggered")  // or use Logger

        val project = e.project ?: return
        val target = TargetResolver.resolveClassTarget(e) ?: return
        RemoteTestExecutor.runRemoteTestClass(project, target.fqName, target.category, target.runTabName)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = VisibilityManager.isClassContext(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}