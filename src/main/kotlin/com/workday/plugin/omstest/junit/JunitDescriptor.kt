package com.workday.plugin.omstest.junit

import com.intellij.execution.Location
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.workday.plugin.omstest.actions.ReRunLastTest
import com.workday.plugin.omstest.util.TestProcessHandler
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
 *         |  |  [TestResultsViewer]           |  |  [ConsoleViewImpl inside SMTRunnerConsoleView]|
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

class JunitDescriptor(
    consoleView: ConsoleView?,
    processHandler: TestProcessHandler,
    component: JComponent,
    displayName: String
) : RunContentDescriptor(consoleView, processHandler, component, displayName) {

    companion object {
        fun createDescriptor(project: Project, runTabName: String): JunitDescriptor {
            val processHandler = TestProcessHandler()
            val consoleView = createConsoleView(project, processHandler)
            val consolePanel = createJunitPanel(consoleView)

            return JunitDescriptor(
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

    }

    fun getMyProcessHandler(): TestProcessHandler = processHandler as TestProcessHandler

    fun getMyConsoleView(): ConsoleView = executionConsole as ConsoleView
}

private object NoOpTestLocator : SMTestLocator {
    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope
    ): List<Location<out PsiElement>> = emptyList()
}
