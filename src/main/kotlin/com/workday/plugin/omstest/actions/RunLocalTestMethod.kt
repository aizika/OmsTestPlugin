package com.workday.plugin.omstest.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.execution.LocalTestExecutor
import com.workday.plugin.omstest.execution.LastTestStorage
import com.workday.plugin.omstest.ui.TestTargetResolver
import com.workday.plugin.omstest.ui.TestTargetResolver.isMethodContext
import java.io.File

/**
 * Action to run a test for the selected Java test method in IntelliJ IDEA against the local OMS.
 * Identifies the method and its containing class at the caret position, then executes the test using Gradle.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunLocalTestMethod : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val target = TestTargetResolver.resolveMethodTarget(event) ?: return
        val targetName = "-PtestMethod=${target.fqName}"
        val commandParts = listOf(
            "./gradlew",
            targetName,
            ":runTestJmx",
            "-s"
        )
        LastTestStorage.setLocal(target.runTabName, targetName)
        val cmdLine = GeneralCommandLine(commandParts)
        cmdLine.workDirectory = File(event.project!!.basePath ?: ".")
        LocalTestExecutor.runLocalCommand(event.project, target.runTabName, targetName)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = isMethodContext(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}