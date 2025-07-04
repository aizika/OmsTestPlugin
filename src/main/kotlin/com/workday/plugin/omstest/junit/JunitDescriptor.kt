package com.workday.plugin.omstest.junit

import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.workday.plugin.omstest.ReRunLastTest
import com.workday.plugin.omstest.util.JunitProcessHandler
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class JunitDescriptor(
    consoleView: ConsoleView?,
    processHandler: JunitProcessHandler,
    component: JComponent,
    displayName: String
) : RunContentDescriptor(consoleView, processHandler, component, displayName) {

    companion object {
        fun createDescriptor(project: Project, runTabName: String): JunitDescriptor {
            val processHandler = JunitProcessHandler()
            val consoleView = ParsedResultConsole().createConsoleView(project, processHandler)

            val consolePanel = createJunitPanel(consoleView, processHandler)

            return JunitDescriptor(
                consoleView,
                processHandler,
                consolePanel,
                runTabName
            )
        }

        fun createJunitPanel(consoleView: ConsoleView, processHandler: JunitProcessHandler): JPanel {
            processHandler.start()

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
    fun getMyProcessHandler(): JunitProcessHandler = processHandler as JunitProcessHandler
    fun getMyConsoleView(): ConsoleView = executionConsole as ConsoleView
    fun getJunitPanel(): JPanel = component as JPanel
}
