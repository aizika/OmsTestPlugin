package com.workday.plugin.omstest.junit

import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.workday.plugin.omstest.parser.Status
import com.workday.plugin.omstest.parser.parseResultFile
import java.io.File
import java.io.OutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ShowTestTreeAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val dummyConfig = DummyRunConfiguration(project)

        val consoleProperties = object : SMTRunnerConsoleProperties(
            dummyConfig,
            "DummyFramework",
            DefaultRunExecutor.getRunExecutorInstance()
        ) {
            override fun getTestLocator(): SMTestLocator = DummyTestLocator
        }

        val consoleView = SMTestRunnerConnectionUtil.createConsole("DummyFramework", consoleProperties)
        val processHandler = DummyProcessHandler()
        consoleView.attachToProcess(processHandler)

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val descriptor = RunContentDescriptor(consoleView, processHandler, consoleView.component, "Dummy Test Run")
        consoleView.allowHeavyFilters()

        val manager = com.intellij.execution.impl.ExecutionManagerImpl.getInstance(project)
        manager.getContentManager().showRunContent(executor, descriptor)

        // Emit test messages from parsed file
        Executors.newSingleThreadScheduledExecutor().schedule({
            val file = File("/mnt/data/TEST-junit-jupiter.xml")
            val results = parseResultFile(file)

            processHandler.notifyTextAvailable(
                "##teamcity[testSuiteStarted name='ParsedSuite']\n",
                ProcessOutputTypes.STDOUT
            )
            for ((_, result) in results) {
                processHandler.notifyTextAvailable(
                    "##teamcity[testStarted name='${result.name}']\n",
                    ProcessOutputTypes.STDOUT
                )

                when (result.status) {
                    Status.FAILED -> processHandler.notifyTextAvailable(
                        "##teamcity[testFailed name='${result.name}' message='${result.failureMessage ?: "Failed"}' details='${
                            result.failureDetails?.replace(
                                "\n",
                                " "
                            ) ?: ""
                        }']\n",
                        ProcessOutputTypes.STDOUT
                    )

                    Status.ERROR -> processHandler.notifyTextAvailable(
                        "##teamcity[testFailed name='${result.name}' message='${result.errorMessage ?: "Error"}' details='${
                            result.errorDetails?.replace(
                                "\n",
                                " "
                            ) ?: ""
                        }']\n",
                        ProcessOutputTypes.STDOUT
                    )

                    Status.SKIPPED -> processHandler.notifyTextAvailable(
                        "##teamcity[testIgnored name='${result.name}' message='${result.skippedMessage ?: "Skipped"}']\n",
                        ProcessOutputTypes.STDOUT
                    )

                    else -> {} // PASSED
                }

                processHandler.notifyTextAvailable(
                    "##teamcity[testFinished name='${result.name}']\n",
                    ProcessOutputTypes.STDOUT
                )
            }
            processHandler.notifyTextAvailable(
                "##teamcity[testSuiteFinished name='ParsedSuite']\n",
                ProcessOutputTypes.STDOUT
            )
            processHandler.destroyProcess()
        }, 500, TimeUnit.MILLISECONDS)
    }
}

// --- Dummy Process Handler ---
class DummyProcessHandler : ProcessHandler() {
    override fun destroyProcessImpl() = notifyProcessTerminated(0)
    override fun detachProcessImpl() = notifyProcessDetached()
    override fun detachIsDefault(): Boolean = false
    override fun getProcessInput(): OutputStream? = null
}

// --- Dummy Test Locator (no hyperlinks) ---
object DummyTestLocator : SMTestLocator {
    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope
    ): List<Location<out PsiElement>> {
        return emptyList()
    }
}

// --- Minimal RunConfiguration infrastructure ---
class DummyRunConfiguration(project: Project) : RunConfigurationBase<RunProfileState>(
    project,
    DummyConfigurationFactory(),
    "DummyRunConfig"
) {
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? = null
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        TODO("Not yet implemented")
    }
}

class DummyConfigurationFactory : ConfigurationFactory(DummyConfigurationType) {
    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return DummyRunConfiguration(project)
    }
}

object DummyConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Dummy"
    override fun getConfigurationTypeDescription(): String = "Dummy configuration type"
    override fun getId(): String = "DUMMY_CONFIG"
    override fun getIcon() = AllIcons.General.Information
    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(DummyConfigurationFactory())
}