package com.workday.plugin.omstest.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiElement
import com.workday.plugin.omstest.remote.RemoteTestExecutor
import com.workday.plugin.omstest.util.TargetResolver
import javax.swing.Icon

class RemoteTestMethodGutterMarkerProvider : LineMarkerProvider {

    private val icon: Icon = IconLoader.getIcon("/icons/omsTestMethodIcon.svg", javaClass)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        val target = TargetResolver.resolveMethodTargetQuietly(element) ?: return null
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