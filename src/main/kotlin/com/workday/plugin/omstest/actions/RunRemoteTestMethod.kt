package com.workday.plugin.omstest.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.execution.RemoteTestExecutor
import com.workday.plugin.omstest.ui.TestTargetResolver
import com.workday.plugin.omstest.ui.TestTargetResolver.isMethodContext

/**
 * Action to run a remote test for the selected Java method in IntelliJ IDEA  against an SUV.
 * Identifies the method at the caret position, then executes the test remotely.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunRemoteTestMethod : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val target = TestTargetResolver.resolveMethodTarget(e) ?: return
        RemoteTestExecutor.runRemoteTest(
            project,
            target.fqName,
            """${target.fqName} empty empty empty ${target.category} /usr/local/workday-oms/logs/junit""",
            target.runTabName
        )
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isMethodContext(e)
    }
}