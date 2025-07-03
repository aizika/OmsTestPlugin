package com.workday.plugin.omstest.local

import LocalProcessHandler
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.workday.plugin.omstest.util.JunitProcessHandler
import com.workday.plugin.omstest.util.LastTestStorage
import com.workday.plugin.omstest.util.TargetResolver
import com.workday.plugin.omstest.util.VisibilityManager
import java.io.File

/**
 * Action to run a Gradle test for the selected Java method in IntelliJ IDEA.
 * Identifies the method and its containing class at the caret position, then executes the test using Gradle.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunGradleTestByMethod : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val target = TargetResolver.resolveMethodTarget(event) ?: return
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

        val processHandler = JunitProcessHandler() // Wrap it
        LocalTestExecutor.runLocalCommand(event.project, target.runTabName, targetName, processHandler)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = VisibilityManager.isMethodContext(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}