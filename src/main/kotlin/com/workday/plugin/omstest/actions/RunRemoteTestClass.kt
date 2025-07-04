package com.workday.plugin.omstest.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.execution.RemoteTestExecutor
import com.workday.plugin.omstest.ui.TargetResolver
import com.workday.plugin.omstest.ui.TargetVisibilityManager

/**
 * Action to run a remote test for the selected Java class in IntelliJ IDEA.
 * Identifies the class at the caret position, then executes the test remotely.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunRemoteTestClass : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        println("RunRemoteTestClass triggered")  // or use Logger

        val project = e.project ?: return
        val target = TargetResolver.resolveClassTarget(e) ?: return
        RemoteTestExecutor.runRemoteTest(
            project,
            target.fqName,
            """empty ${target.fqName} empty empty ${target.category} /usr/local/workday-oms/logs/junit""",
            target.runTabName
        )
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = TargetVisibilityManager.isClassContext(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}