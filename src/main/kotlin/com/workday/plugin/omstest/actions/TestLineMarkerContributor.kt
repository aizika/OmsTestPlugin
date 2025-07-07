package com.workday.plugin.omstest.actions

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.workday.plugin.omstest.execution.LastTestStorage
import com.workday.plugin.omstest.execution.LocalTestExecutor
import com.workday.plugin.omstest.execution.RemoteTestExecutor
import com.workday.plugin.omstest.ui.TestTargetResolver.getTestCategory
import java.io.File

/**
 * Adds OMS menu items to the Intellij test popup menu.
 * This menu pops up when a user clicks on Intellij test icons in the gutter.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class TestLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null

        val parent = element.parent
        val project = element.project

        return when (parent) {
            is PsiMethod -> {
                val clazz = parent.containingClass ?: return null
                val category = getTestCategory(clazz)

                val methodName = "${clazz.qualifiedName}@${parent.name}"

                val runLocal = object : AnAction("Run Local: ${parent.name}") {
                    override fun actionPerformed(e: AnActionEvent) {
                        val param = "-PtestMethod=$methodName"
                        LastTestStorage.setLocal(methodName, param)
                        val cmd = GeneralCommandLine(listOf("./gradlew", param, ":runTestJmx", "-s"))
                        cmd.workDirectory = File(e.project?.basePath ?: ".")
                        LocalTestExecutor.runLocalCommand(e.project, parent.name, param)
                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.isEnabledAndVisible = true
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                }

                val runRemote = object : AnAction("Run Remote: ${parent.name}") {

                    override fun actionPerformed(e: AnActionEvent) {
                        RemoteTestExecutor.runRemoteTest(
                            project,
                            methodName,
                            """${methodName} empty empty empty ${category} /usr/local/workday-oms/logs/junit""",
                            parent.name
                        )

                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.isEnabledAndVisible = true
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                }

                Info(
                    AllIcons.RunConfigurations.TestState.Run,
                    { "Run OMS test method" },
                    runLocal, runRemote
                )
            }

            is PsiClass -> {
                val fqName = parent.qualifiedName ?: return null
                val category = getTestCategory(parent)

                val runLocal = object : AnAction("Run Local (Class): ${parent.name}") {
                    override fun actionPerformed(e: AnActionEvent) {
                        val param = "-PtestClass=$fqName"
                        LastTestStorage.setLocal(fqName, param)
                        val cmd = GeneralCommandLine(listOf("./gradlew", param, ":runTestJmx", "-s"))
                        cmd.workDirectory = File(e.project?.basePath ?: ".")
                        LocalTestExecutor.runLocalCommand(e.project, parent.name!!, param)
                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.isEnabledAndVisible = true
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                }

                val runRemote = object : AnAction("Run Remote (Class): ${parent.name}") {
                    override fun actionPerformed(e: AnActionEvent) {
//                        RemoteTestExecutor.runRemoteTest(e.project, fqName, )
                        RemoteTestExecutor.runRemoteTest(
                            project,
                            fqName,
                            """empty ${fqName} empty empty ${category} /usr/local/workday-oms/logs/junit""",
                            parent.name!!)

                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.isEnabledAndVisible = true
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                }

                Info(
                    AllIcons.RunConfigurations.TestState.Run,
                    { "Run OMS test class" },
                    runLocal, runRemote
                )
            }

            else -> null
        }
    }
}