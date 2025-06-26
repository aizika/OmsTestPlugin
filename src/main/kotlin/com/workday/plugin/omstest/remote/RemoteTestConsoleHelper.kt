package com.workday.plugin.omstest.remote

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.run.DummyRunConfiguration
import com.workday.plugin.omstest.run.RemoteTestProcessHandler

object RemoteTestConsoleHelper {

    fun showTestConsole(project: Project, title: String): Pair<RemoteTestProcessHandler, RunContentDescriptor> {
        val handler = RemoteTestProcessHandler()

        val config = DummyRunConfiguration(project)
        val props = SMTRunnerConsoleProperties(config, "RemoteTest", DefaultRunExecutor.getRunExecutorInstance())
        props.isIdBasedTestTree = true

        val consoleView = SMTestRunnerConnectionUtil.createAndAttachConsole("RemoteTest", handler, props)

        val descriptor = RunContentDescriptor(consoleView, handler, consoleView.component, title)
        RunContentManager.getInstance(project).showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)

        handler.startNotify()
        return handler to descriptor
    }
}