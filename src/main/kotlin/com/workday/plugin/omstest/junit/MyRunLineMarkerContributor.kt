package com.workday.plugin.omstest.junit

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod

class MyRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        val actionManager = ActionManager.getInstance()
        val methodGroup = actionManager.getAction("OmsTestMethodGroup") as? DefaultActionGroup ?: return null
        val methodGroupActions = methodGroup.getChildren(null)

        val classGroup = actionManager.getAction("OmsTestClassGroup") as? DefaultActionGroup ?: return null
        val classGroupActions = classGroup.getChildren(null)

        if (element is PsiIdentifier) {
            when (element.parent) {
                is PsiMethod -> {
                    return Info(
                        AllIcons.RunConfigurations.TestState.Run,
                        { "Run OMS test method" },
                        *methodGroupActions
                    )
                }
                is PsiClass -> {
                    return Info(
                        AllIcons.RunConfigurations.TestState.Run,
                        { "Run OMS test class" },
                        *classGroupActions
                    )
                }
            }
        }

        return null
    }
}