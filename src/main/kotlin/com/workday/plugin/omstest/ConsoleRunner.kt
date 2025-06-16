package com.workday.plugin.omstest


import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Utility object for running external commands and displaying their output in an IntelliJ IDEA console tab.
 * Stores the last executed command and label for rerun functionality.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
object ConsoleRunner {
    fun runCommand(command: List<String>, project: Project?, tabName: String) {
        LastTestStorage.lastCommand = command
        LastTestStorage.lastLabel = tabName

        if (project == null) return

        val cmdLine = GeneralCommandLine(command)
        cmdLine.workDirectory = File(project.basePath ?: ".")

        val processHandler = OSProcessHandler(cmdLine)
        val consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .console
        consoleView.attachToProcess(processHandler)

        val descriptor = RunContentDescriptor(consoleView, processHandler, consoleView.component, tabName)

        val executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
        val runContentManager = RunContentManager.getInstance(project)

        runContentManager.showRunContent(executor, descriptor)

        processHandler.startNotify()
    }
}