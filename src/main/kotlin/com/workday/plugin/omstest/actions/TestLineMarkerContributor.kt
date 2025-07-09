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
import com.workday.plugin.omstest.ui.TestTargetResolver.isOmsTestClass
import com.workday.plugin.omstest.ui.TestTargetResolver.isOmsTestMethod
import java.io.File

/**
 * Adds OMS menu items to the Intellij test popup menu.
 * This menu pops up when a user clicks on Intellij test icons in the gutter.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class TestLineMarkerContributor : RunLineMarkerContributor() {

    private val logger = com.intellij.openapi.diagnostic.Logger.getInstance(TestLineMarkerContributor::class.java)

    override fun getInfo(element: PsiElement): Info? {
        logger.info("getInfo called for element: ${element::class.simpleName} - ${element.text}")

        if (element !is PsiIdentifier) {
            logger.debug("Element is not PsiIdentifier: ${element::class.simpleName}")
            return null
        }

        val parent = element.parent
        val project = element.project

        return when (parent) {
            is PsiMethod -> {
                logger.debug("PsiIdentifier parent is method: ${parent.name}")
                if (!isOmsTestMethod(parent)) {
                    logger.debug("Method ${parent.name} is not a valid OMS test method")
                    return null
                }

                val clazz = parent.containingClass ?: run {
                    logger.debug("Method ${parent.name} has no containing class")
                    return null
                }

                if (!isOmsTestClass(clazz)) {
                    logger.debug("Class ${clazz.qualifiedName} is not a valid OMS test class")
                    return null
                }

                val category = getTestCategory(clazz)
                val methodName = "${clazz.qualifiedName}@${parent.name}"
                logger.debug("Creating actions for test method: $methodName")

                val runLocal = object : AnAction("Run Local: ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
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

                val runRemote = object : AnAction("Run Remote: ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
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

                logger.debug("Returning Info with actions for method $methodName")
                Info(AllIcons.RunConfigurations.TestState.Run, { "Run OMS test method" }, runLocal, runRemote)
            }

            is PsiClass -> {
                logger.debug("PsiIdentifier parent is class: ${parent.qualifiedName}")
                if (!isOmsTestClass(parent)) {
                    logger.debug("Class ${parent.qualifiedName} is not a valid OMS test class")
                    return null
                }

                val fqName = parent.qualifiedName ?: return null
                val category = getTestCategory(parent)

                val runLocal = object : AnAction("Run Local (Class): ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
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

                val runRemote = object : AnAction("Run Remote (Class): ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
                    override fun actionPerformed(e: AnActionEvent) {
                        RemoteTestExecutor.runRemoteTest(
                            project,
                            fqName,
                            """empty ${fqName} empty empty ${category} /usr/local/workday-oms/logs/junit""",
                            parent.name!!
                        )
                    }

                    override fun update(e: AnActionEvent) {
                        e.presentation.isEnabledAndVisible = true
                    }

                    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                }

                logger.debug("Returning Info with actions for class $fqName")
                Info(AllIcons.RunConfigurations.TestState.Run, { "Run OMS test class" }, runLocal, runRemote)
            }

            else -> {
                logger.debug("Element parent is neither method nor class: ${parent::class.simpleName}")
                null
            }
        }
    }
}