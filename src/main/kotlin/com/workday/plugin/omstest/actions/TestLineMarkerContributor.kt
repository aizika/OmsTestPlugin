package com.workday.plugin.omstest.actions

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
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

class TestLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element !is PsiIdentifier) return null

        val parent = element.parent
        return when (parent) {
            is PsiMethod -> getMethodInfo(parent, element.project)
            is PsiClass -> getClassInfo(parent, element.project)
            else -> null
        }
    }

    private fun getMethodInfo(method: PsiMethod, project: com.intellij.openapi.project.Project): Info? {
        if (!isOmsTestMethod(method)) return null
        val clazz = method.containingClass ?: return null
        if (!isOmsTestClass(clazz)) return null

        val methodSignature = buildMethodSignature(clazz.qualifiedName!!, method)
        val methodName = "${clazz.qualifiedName}@${method.name}"
        val category = getTestCategory(clazz)

        val runLocal = createRunAction("Run Local: ${method.name}", methodName, methodSignature, project, true)
        val runRemote =
            createRunAction("Run Remote: ${method.name}", methodName, methodSignature, project, false, category)

        return Info(AllIcons.RunConfigurations.TestState.Run, { "Run OMS Test Method" }, runLocal, runRemote)
    }

    private fun getClassInfo(clazz: PsiClass, project: com.intellij.openapi.project.Project): Info? {
        if (!isOmsTestClass(clazz)) return null

        val fqName = clazz.qualifiedName ?: return null
        val category = getTestCategory(clazz)

        val runLocal = createRunAction("Run Local (Class): ${clazz.name}", fqName, "-PtestClass=$fqName", project, true)
        val runRemote = createRunAction("Run Remote (Class): ${clazz.name}", fqName, fqName, project, false, category)

        return Info(AllIcons.RunConfigurations.TestState.Run, { "Run OMS Test Class" }, runLocal, runRemote)
    }

    private fun createRunAction(
        title: String,
        name: String,
        param: String,
        project: com.intellij.openapi.project.Project,
        isLocal: Boolean,
        category: String = ""
    ): AnAction {
        return object : AnAction(title, null, AllIcons.RunConfigurations.TestState.Run) {
            override fun actionPerformed(e: AnActionEvent) {
                if (isLocal) {
                    LastTestStorage.setLocal(name, param)
                    LocalTestExecutor.runLocalCommand(project, name, param)
                } else {
                    RemoteTestExecutor.runRemoteTest(
                        project,
                        name,
                        """$param empty empty empty $category /usr/local/workday-oms/logs/junit""",
                        name
                    )
                }
            }
        }
    }

    private fun buildMethodSignature(classFqName: String, method: PsiMethod): String {
        val paramTypes = method.parameterList.parameters.joinToString(",") { it.type.canonicalText }
        return if (paramTypes.isBlank()) "$classFqName@${method.name}" else "$classFqName@${method.name}\\($paramTypes\\)"
    }
}