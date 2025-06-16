package com.workday.plugin.omstest

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.psi.JavaDirectoryService
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager

/**
 * Action to run a Gradle test for all tests in the selected Java package in IntelliJ IDEA.
 * Determines the package from the current context and executes the test using Gradle.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
class RunGradleTestByPackage : AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val psiManager = PsiManager.getInstance(project)

        val psiFile = event.getData(CommonDataKeys.PSI_FILE)
        val psiDirectory = event.getData(CommonDataKeys.PSI_ELEMENT) as? PsiDirectory
            ?: psiFile?.containingDirectory
            ?: event.getData(CommonDataKeys.VIRTUAL_FILE)?.let { vf ->
                if (vf.isDirectory) psiManager.findDirectory(vf) else null
            }

        val packageName = psiDirectory?.let { JavaDirectoryService.getInstance().getPackage(it)?.qualifiedName }
            ?: return

        val commandParts = listOf("./gradlew", "-PtestPackage=$packageName", ":runTestJmx", "-s")
        ConsoleRunner.runCommand(commandParts, project, "Package: $packageName")
    }
}