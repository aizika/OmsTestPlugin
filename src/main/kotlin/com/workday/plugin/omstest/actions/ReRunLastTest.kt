package com.workday.plugin.omstest.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.workday.plugin.omstest.execution.LocalTestExecutor
import com.workday.plugin.omstest.execution.RemoteTestExecutor
import com.workday.plugin.omstest.execution.LastTestStorage
import java.io.File

/**
 * Action to re-run the last executed test in IntelliJ IDEA.
 * It checks the environment (local or remote) and executes the appropriate command.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class ReRunLastTest : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        when (LastTestStorage.environment) {
            LastTestStorage.Environment.LOCAL -> {
                val runTabName = LastTestStorage.runTabName
                val targetName = LastTestStorage.targetName

                if (runTabName == null || targetName == null) {
                    showMissingDialog(project)
                    return
                }
                val commandParts = listOf("./gradlew", targetName, ":runTestJmx", "-s")
                LastTestStorage.setLocal(runTabName, targetName)

                val cmdLine = GeneralCommandLine(commandParts)
                cmdLine.workDirectory = File(project.basePath ?: ".")
                LocalTestExecutor.runLocalCommand(project, runTabName, targetName)
            }

            LastTestStorage.Environment.REMOTE -> {
                val fqTestName = LastTestStorage.fqTestName
                val jmxParams = LastTestStorage.jmxParams
                val host = LastTestStorage.host
                val runTabName = LastTestStorage.runTabName ?: "Run test"

                if (fqTestName == null || jmxParams == null || host == null) {
                    showMissingDialog(project)
                    return
                }

                RemoteTestExecutor.runTestWithHost(
                    project,
                    fqTestName,
                    jmxParams,
                    host,
                    runTabName
                )

            }

            null -> {
                showMissingDialog(project)
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = LastTestStorage.environment != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    private fun showMissingDialog(project: Project) {
        Messages.showWarningDialog(
            project,
            "No previous test found to re-run.",
            "Re-Run Last Test"
        )
    }
}