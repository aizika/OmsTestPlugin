package com.workday.plugin.omstest.ui

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
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.workday.plugin.omstest.actions.ReRunLastTest
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * This class represents a UI content descriptor for JUnit test results in Run Tool Window.
 *
 *         +--------------------------------------------------------------------------------------+
 *         |                       Run Tool Window Tab ("OMS Test Results")                       |
 *         |                        [Owned by RunContentDescriptor]                               |
 *         +--------------------------------------------------------------------------------------+
 *         |  Toolbar (left top corner)                                                           |
 *         |  [Created by ActionGroup + Executor + ExecutionManager UI helpers]                   |
 *         |                                                                                      |
 *         |  +--------------------------------+  +----------------------------------------+      |
 *         |  |   Test Tree Panel              |  |        Console Output Panel            |      |
 *         |  |  [TestResultsViewer]           |  |  [ConsoleView inside SMTRunnerConsoleView]    |
 *         |  |  - Based on SMTestProxy tree   |  |  - Receives process output, SM logs    |      |
 *         |  |  - Updates via SM events       |  |  - Printed system output, test logs    |      |
 *         |  +--------------------------------+  +----------------------------------------+      |
 *         |      ▲                                                                         ▲     |
 *         |      |                                                                         |     |
 *         |      |        [SMTRunnerConsoleView manages both left and right panes]         |     |
 *         +--------------------------------------------------------------------------------------+
 *
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */

class UiContentDescriptor(
    consoleView: ConsoleView?,
    processHandler: UiProcessHandler,
    component: JComponent,
    displayName: String
) : RunContentDescriptor(consoleView, processHandler, component, displayName) {

    companion object {
        fun createDescriptor(project: Project, runTabName: String): UiContentDescriptor {
            val processHandler = UiProcessHandler()
            val consoleView = createConsoleView(project, processHandler)
            val consolePanel = createJunitPanel(consoleView)

            return UiContentDescriptor(
                consoleView,
                processHandler,
                consolePanel,
                runTabName
            )
        }

        fun createConsoleView(project: Project, handler: ProcessHandler): ConsoleView {
            val config = ParsedResultRunConfiguration(project)
            val consoleProperties = object : SMTRunnerConsoleProperties(
                config,
                "ParsedResults",
                DefaultRunExecutor.getRunExecutorInstance()
            ) {
                override fun getTestLocator(): SMTestLocator = NoOpTestLocator
            }

            val view = SMTestRunnerConnectionUtil.createConsole("ParsedResults", consoleProperties)
            view.attachToProcess(handler)
            return view
        }

        fun createJunitPanel(consoleView: ConsoleView): JPanel {

            val actionGroup = DefaultActionGroup().apply {
                add(ReRunLastTest())
            }
            val toolbar = ActionManager.getInstance().createActionToolbar("GradleTestToolbar", actionGroup, false)

            // Wrap console and toolbar in a panel
            val consolePanel = JPanel(BorderLayout())
            consolePanel.add(toolbar.component, BorderLayout.WEST)
            consolePanel.add(consoleView.component, BorderLayout.CENTER)
            return consolePanel
        }

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
    }

    fun getUiProcessHandler(): UiProcessHandler = processHandler as UiProcessHandler

    fun getConsoleView(): ConsoleView = executionConsole as ConsoleView
}

private object NoOpTestLocator : SMTestLocator {
    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope
    ): List<Location<out PsiElement>> = emptyList()
}
