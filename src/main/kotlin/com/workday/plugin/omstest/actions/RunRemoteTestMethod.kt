package com.workday.plugin.omstest.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.remote.RemoteTestExecutor
import com.workday.plugin.omstest.util.TargetResolver
import com.workday.plugin.omstest.util.VisibilityManager

/**
 * Action to run a remote test for the selected Java method in IntelliJ IDEA.
 * Identifies the method at the caret position, then executes the test remotely.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunRemoteTestMethod : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val target = TargetResolver.resolveMethodTarget(e) ?: return
        RemoteTestExecutor.runRemoteTest(
            project,
            target.fqName,
            """${target.fqName} empty empty empty OMSBI /usr/local/workday-oms/logs/junit""",
            target.runTabName
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = VisibilityManager.isMethodContext(e)
    }
}