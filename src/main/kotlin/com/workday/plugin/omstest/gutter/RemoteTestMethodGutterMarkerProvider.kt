package com.workday.plugin.omstest.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.workday.plugin.omstest.remote.RemoteTestExecutor
import com.workday.plugin.omstest.util.TargetResolver
import javax.swing.Icon

class RemoteTestMethodGutterMarkerProvider : LineMarkerProvider {

    private val icon: Icon = IconLoader.getIcon("/icons/omsTestMethodIcon.svg", javaClass)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Check if the element is a method identifier
        val method = (element.parent as? PsiMethod) ?: return null
        if (element != method.nameIdentifier) return null

        // Optional: only annotate @Test methods
        val isTestMethod = method.annotations.any {
            it.qualifiedName == "org.junit.Test" || it.qualifiedName == "org.junit.jupiter.api.Test"
        }
        if (!isTestMethod) return null
        val target = TargetResolver.resolveMethodTargetFromElement(element) ?: return null

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "Run remote OMS test" },
            { _, elt ->
                RemoteTestExecutor.runRemoteTestMethod(elt.project, target.fqName, target.runTabName)
            },
            GutterIconRenderer.Alignment.LEFT,
            { "Run remote OMS test" }
        )
    }
}