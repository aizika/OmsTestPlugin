package com.workday.plugin.omstest.local


import com.intellij.execution.Executor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.util.LastTestStorage
import java.io.File

/**
 * Utility object for running external commands and displaying their output in an IntelliJ IDEA console tab.
 * Stores the last executed command and label for rerun functionality.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
object LocalTestExecutor {

    /**
     * Runs a command in the local environment and displays the output in a console tab.
     *
     * @param project The IntelliJ IDEA project context.
     * @param runTabName A label for the Run tool window tab where the test output will be displayed.
     */
    fun runCommand(project: Project?, runTabName: String, targetName: String) {
        if (project == null) return

        val commandParts = listOf(
            "./gradlew",
            targetName,
            ":runTestJmx",
            "-s"
        )
        LastTestStorage.setLocal(runTabName, targetName)

        val cmdLine = GeneralCommandLine(commandParts)
        cmdLine.workDirectory = File(project.basePath ?: ".")

        val processHandler = OSProcessHandler(cmdLine)
        val consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .console
        consoleView.attachToProcess(processHandler)

        val descriptor = RunContentDescriptor(
            consoleView,
            processHandler,
            consoleView.component,
            "$runTabName locally"
        )

        val executor: Executor = DefaultRunExecutor.getRunExecutorInstance()
        val runContentManager = RunContentManager.getInstance(project)

        runContentManager.showRunContent(executor, descriptor)

        processHandler.startNotify()
    }
}