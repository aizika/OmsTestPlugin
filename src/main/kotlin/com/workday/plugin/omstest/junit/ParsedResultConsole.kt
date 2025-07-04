package com.workday.plugin.omstest.junit

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import javax.swing.JPanel


/**
 * RunConfiguration used for displaying parsed JUnit results in the test UI.
 */
class ParsedResultRunConfiguration(project: Project) : RunConfigurationBase<RunProfileState>(
    project,
    ParsedResultConfigFactory,
    "ParsedRunConfig"
) {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return object : SettingsEditor<RunConfiguration>() {
            override fun resetEditorFrom(s: RunConfiguration) {}
            override fun applyEditorTo(s: RunConfiguration) {}
            override fun createEditor() = JPanel()
        }
    }
}

/**
 * Factory for ParsedResultRunConfiguration.
 */
object ParsedResultConfigFactory : ConfigurationFactory(ParsedResultConfigType) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return ParsedResultRunConfiguration(project)
    }
}

/**
 * Configuration type metadata for parsed test result viewer.
 */
object ParsedResultConfigType : ConfigurationType {
    override fun getDisplayName(): String = "Parsed Test Viewer"
    override fun getConfigurationTypeDescription(): String = "Displays parsed JUnit results"
    override fun getId(): String = "PARSED_TEST_RESULT_VIEWER"
    override fun getIcon() = AllIcons.General.Information
    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(ParsedResultConfigFactory)
}

