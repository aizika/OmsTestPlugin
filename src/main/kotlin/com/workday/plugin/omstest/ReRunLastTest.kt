package com.workday.plugin.omstest

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

/**
 * Action to re-run the last executed test command in IntelliJ IDEA.
 * Checks for a stored command and label, and triggers execution if available.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class ReRunLastTest : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val command = LastTestStorage.lastCommand
        val label = LastTestStorage.lastLabel

        if (command == null || label == null) {
            Messages.showWarningDialog(
                project,
                "No previous test command found.",
                "Re-Run Last Test"
            )
            return
        }

        ConsoleRunner.runCommand(command, project, label)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = LastTestStorage.lastCommand != null
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT  // Or EDT depending on UI interaction
    }
}
