package com.workday.plugin.omstest.remote

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.util.TargetResolver
import com.workday.plugin.omstest.util.VisibilityManager

class RunRemoteTestByMethod : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val target = TargetResolver.resolveMethodTarget(e) ?: return
        RemoteTestExecutor.runRemoteTestMethod(project, target.fqName, target.runTabName)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = VisibilityManager.isMethodContext(e)
    }
}