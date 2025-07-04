package com.workday.plugin.omstest.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

/**
 * Utility to determine the visibility context of actions in the plugin.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */

object TargetVisibilityManager {

    fun isClassContext(e: AnActionEvent): Boolean {
        val selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (selectedElement is PsiClass) return true

        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile != null && editor != null) {
            val offset = editor.caretModel.offset
            val element = psiFile.findElementAt(offset)
            if (PsiTreeUtil.getParentOfType(element, PsiClass::class.java) != null) return true
        }

        return false
    }

    fun isMethodContext(e: AnActionEvent): Boolean {
        val selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT)
        if (selectedElement is PsiMethod) return true

        val psiFile = e.getData(CommonDataKeys.PSI_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (psiFile != null && editor != null) {
            val offset = editor.caretModel.offset
            val element = psiFile.findElementAt(offset)
            if (PsiTreeUtil.getParentOfType(element, PsiMethod::class.java) != null) return true
        }

        return false
    }
}