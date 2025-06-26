package com.workday.plugin.omstest.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.options.SettingsEditor
import com.intellij.execution.configurations.RunConfiguration
import javax.swing.JPanel
import com.intellij.execution.Executor
import com.intellij.execution.runners.ExecutionEnvironment

class RemoteTestRunConfiguration(project: Project, factory: ConfigurationFactory) :
    RunConfigurationBase<Any?>(project, factory, "RemoteTest") {

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        object : SettingsEditor<RunConfiguration>() {
            override fun resetEditorFrom(s: RunConfiguration) {}
            override fun applyEditorTo(s: RunConfiguration) {}
            override fun createEditor() = JPanel()
        }

    override fun checkConfiguration() {}
    override fun getState(executor: Executor, env: ExecutionEnvironment) = null
}