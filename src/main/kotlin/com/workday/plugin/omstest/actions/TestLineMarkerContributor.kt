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
        val elementText = element.text.take(200)
        logger.info("GetInfo Called For: ${element::class.simpleName} – \"$elementText\"")

        if (element !is PsiIdentifier) {
            logger.debug("Element Is Not PsiIdentifier: ${element::class.simpleName}")
            return null
        }

        val parent = element.parent
        val project = element.project

        return when (parent) {
            is PsiMethod -> {
                logger.debug("Parent Is PsiMethod: ${parent.name}")
                if (!isOmsTestMethod(parent)) {
                    logger.debug("Method ${parent.name} Is Not A Valid OMS Test Method")
                    return null
                }

                val clazz = parent.containingClass ?: run {
                    logger.debug("Method ${parent.name} Has No Containing Class")
                    return null
                }

                if (!isOmsTestClass(clazz)) {
                    logger.debug("Class ${clazz.qualifiedName} Is Not A Valid OMS Test Class")
                    return null
                }

                val category = getTestCategory(clazz)
                val methodName = "${clazz.qualifiedName}@${parent.name}"

                val methodSignature = buildMethodSignature(clazz.qualifiedName!!, parent)
                logger.info("✔ Found OMS Test Method: $methodName, signature: $methodSignature, category: $category")

                val runLocal =
                    object : AnAction("Run Local: ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
                        override fun actionPerformed(e: AnActionEvent) {
                            val param = "-PtestMethod='$methodSignature'"
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

                val runRemote =
                    object : AnAction("Run Remote: ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
                        override fun actionPerformed(e: AnActionEvent) {
                            RemoteTestExecutor.runRemoteTest(
                                project,
                                methodName,
                                """${methodSignature} empty empty empty ${category} /usr/local/workday-oms/logs/junit""",
                                parent.name
                            )
                        }

                        override fun update(e: AnActionEvent) {
                            e.presentation.isEnabledAndVisible = true
                        }

                        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
                    }

                Info(AllIcons.RunConfigurations.TestState.Run, { "Run OMS Test Method" }, runLocal, runRemote)
            }

            is PsiClass -> {
                logger.debug("Parent Is PsiClass: ${parent.qualifiedName}")
                if (!isOmsTestClass(parent)) {
                    logger.debug("Class ${parent.qualifiedName} Is Not A Valid OMS Test Class")
                    return null
                }

                val fqName = parent.qualifiedName ?: return null
                val category = getTestCategory(parent)
                logger.info("✔ Found OMS Test Class: $fqName")

                val runLocal = object :
                    AnAction("Run Local (Class): ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
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

                val runRemote = object :
                    AnAction("Run Remote (Class): ${parent.name}", null, AllIcons.RunConfigurations.TestState.Run) {
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

                Info(AllIcons.RunConfigurations.TestState.Run, { "Run OMS Test Class" }, runLocal, runRemote)
            }

            else -> {
                logger.debug("Element Parent Is Neither Method Nor Class: ${parent::class.simpleName}")
                null
            }
        }
    }
    fun buildMethodSignature(classFqName: String, method: PsiMethod): String {
        val paramTypes = method.parameterList.parameters
            .joinToString(",") { it.type.canonicalText }

        return if (paramTypes.isBlank()) {
            "$classFqName@${method.name}"
        } else {
            "$classFqName@${method.name}\\($paramTypes\\)"
        }
    }
}