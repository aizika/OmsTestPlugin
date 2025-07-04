package com.workday.plugin.omstest.junit

import com.intellij.execution.process.ProcessHandler
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

class MyDescriptor(
    consoleView: ConsoleView?,
    processHandler: ProcessHandler?,
    component: JComponent,
    displayName: String
) : RunContentDescriptor(consoleView, processHandler, component, displayName) {

    companion object {
        fun createDescriptor(project: Project, runTabName: String): MyDescriptor {
            val processHandler = JunitProcessHandler()
            val consoleView = ParsedResultConsole().createConsoleView(project, processHandler)

            val consolePanel = createJunitPanel(consoleView, processHandler)

            return MyDescriptor(
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
}
