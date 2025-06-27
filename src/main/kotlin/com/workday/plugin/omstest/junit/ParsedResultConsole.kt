package com.workday.plugin.omstest.junit

import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.execution.ui.RunContentManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import java.io.OutputStream
import javax.swing.JPanel

/**
 * Creates and displays a test console UI backed by a dummy run configuration for parsed test results.
 */
class ParsedResultConsole {

    var consoleView: ConsoleView? = null
        private set

    var processHandler: ProcessHandler? = null
        private set

    fun show(project: Project): Pair<ConsoleView, ProcessHandler> {
        val (view, handler) = createConsole1(project)
        consoleView = view
        processHandler = handler

        val descriptor = RunContentDescriptor(view, handler, view.component, "Parsed Test Results")
        RunContentManager.getInstance(project)
            .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor)
        return Pair(view, handler)
    }

    fun createConsole1(project: Project): Pair<ConsoleView, ProcessHandler> {
        val config = ParsedResultRunConfiguration(project)
        val consoleProperties = object : SMTRunnerConsoleProperties(
            config,
            "ParsedResults",
            DefaultRunExecutor.getRunExecutorInstance()
        ) {
            override fun getTestLocator(): SMTestLocator = NoOpTestLocator
        }

        val view = SMTestRunnerConnectionUtil.createConsole("ParsedResults", consoleProperties)
        val handler = object : ProcessHandler() {
            override fun destroyProcessImpl() = notifyProcessTerminated(0)
            override fun detachProcessImpl() = notifyProcessDetached()
            override fun detachIsDefault(): Boolean = false
            override fun getProcessInput(): OutputStream? = null
        }

        view.attachToProcess(handler)
        return Pair(view, handler)
    }

    private object NoOpTestLocator : SMTestLocator {
        override fun getLocation(
            protocol: String,
            path: String,
            project: Project,
            scope: GlobalSearchScope
        ): List<Location<out PsiElement>> = emptyList()
    }
}

/**
 * Dummy RunConfiguration used for displaying parsed JUnit results in the test UI.
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

