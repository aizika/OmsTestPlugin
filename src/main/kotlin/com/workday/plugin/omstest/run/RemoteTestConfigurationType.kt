package com.workday.plugin.omstest.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import javax.swing.Icon

class RemoteTestConfigurationType : ConfigurationType {
    override fun getDisplayName() = "Remote Test"
    override fun getConfigurationTypeDescription() = "Remote Test Configuration"
    override fun getIcon(): Icon = AllIcons.RunConfigurations.Junit
    override fun getId() = "REMOTE_TEST_RUN_CONFIGURATION"
    override fun getConfigurationFactories() = arrayOf(RemoteTestConfigurationFactory(this))

    class RemoteTestConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
        override fun createTemplateConfiguration(project: Project): RunConfiguration =
            RemoteTestRunConfiguration(project, this)

        override fun getSingletonPolicy() = RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY
    }
}