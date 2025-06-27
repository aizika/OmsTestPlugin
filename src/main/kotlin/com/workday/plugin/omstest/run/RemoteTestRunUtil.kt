package com.workday.plugin.omstest.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.RunContentBuilder
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.ui.ConsoleView
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import javax.swing.JPanel

class DummyRunConfiguration(project: Project) :
    RunConfigurationBase<Any>(project, object : ConfigurationFactory(object : ConfigurationType {
        override fun getDisplayName() = "Dummy"
        override fun getConfigurationTypeDescription() = "Dummy config"
        override fun getIcon() = AllIcons.General.Information
        override fun getId() = "DUMMY_ID"
        override fun getConfigurationFactories(): Array<out ConfigurationFactory> = emptyArray()
//        override fun getConfigurationFactories() = arrayOf()
    }) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration {
            TODO("Not yet implemented")
        }
    }, "Dummy") {

    override fun getConfigurationEditor() = object : SettingsEditor<RunConfiguration>() {
        override fun resetEditorFrom(s: RunConfiguration) {}
        override fun applyEditorTo(s: RunConfiguration) {}
        override fun createEditor() = JPanel()
    }

    override fun checkConfiguration() {}
    override fun getState(
        executor: Executor,
        environment: ExecutionEnvironment
    ): RunProfileState? {
        TODO("Not yet implemented")
    }
}

fun startTestExecutionUI(project: Project): Pair<RemoteTestProcessHandler, ConsoleView> {
    val handler = RemoteTestProcessHandler()
    val configType = RemoteTestConfigurationType()
    val factory = configType.configurationFactories[0]
    val config = RemoteTestRunConfiguration(project, factory)

    val executor = DefaultRunExecutor.getRunExecutorInstance()
    val environment = ExecutionEnvironmentBuilder.create(executor, config).build()

    val properties = SMTRunnerConsoleProperties(config, "RemoteTest", executor)
    val consoleProps = object : SMTRunnerConsoleProperties( DummyRunConfiguration(project), "RemoteTest", DefaultRunExecutor.getRunExecutorInstance()) {
        override fun isDebug() = false

    }

    val consoleView = SMTestRunnerConnectionUtil.createAndAttachConsole("RemoteTest", handler, consoleProps)

    // ✅ Launch the UI
    ProgramRunnerUtil.executeConfiguration(environment, false, true)
    val executionResult = DefaultExecutionResult(consoleView, handler)
    val descriptor = RunContentBuilder(executionResult, environment).showRunContent(null)
//    val descriptor = RunContentBuilder(executionResult, environment).showRunContent(DefaultRunExecutor.getRunExecutorInstance())

    return Pair(handler, consoleView)
}