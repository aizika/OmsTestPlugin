package com.workday.plugin.omstest.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.workday.plugin.omstest.remote.RemoteTestExecutor
import com.workday.plugin.omstest.util.TargetResolver
import javax.swing.Icon

/**
 * Provides a gutter marker for running remote OMS class tests.
 * This marker appears next to class declarations in the editor.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RemoteTestClassGutterMarkerProvider : LineMarkerProvider {

    private val icon: Icon = IconLoader.getIcon("/icons/omsTestClassIcon.svg", javaClass)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiIdentifier || element.parent !is PsiClass) return null

        val target = TargetResolver.resolveClassTargetQuietly(element) ?: return null

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "Run remote OMS class test" },
            { _, elt ->
                RemoteTestExecutor.runRemoteTestClass(elt.project, target.fqName, target.category, target.runTabName)
            },
            GutterIconRenderer.Alignment.LEFT,
            { "Run remote OMS class test" }
        )
    }
}