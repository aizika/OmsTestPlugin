package com.workday.plugin.testrunner.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;

import com.workday.plugin.testrunner.common.HostPromptDialog;
import com.workday.plugin.testrunner.common.LastTestStorage;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.execution.OSCommands;
import com.workday.plugin.testrunner.execution.OrsRunStrategy;
import com.workday.plugin.testrunner.execution.ParamBuilder;
import com.workday.plugin.testrunner.execution.TestRunner;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

public class ProjectViewSuvJmxAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        ProjectViewRunGroup.OmsTarget target = ProjectViewRunGroup.getTarget(e);
        if (target == null) return;

        HostPromptDialog dialog = new HostPromptDialog();
        if (!dialog.showAndGet()) return;
        String host = dialog.getHost();
        if (host.isBlank()) {
            ReRunLastTestAction.showBalloon(project, "No valid host specified");
            return;
        }

        String[] jmxParams;
        if (target.isPackage()) {
            String category = ProjectViewRunGroup.promptCategory(project);
            if (category == null) return;
            jmxParams = ParamBuilder.getPackageArgs(target.packageName(), category);
        } else {
            jmxParams = ParamBuilder.getClassArgs(target.classFqn());
        }

        Locations.setBasePath(project.getBasePath());
        final String tabName = target.shortName() + "@ors:" + host.replaceFirst("\\.workdaysuv\\.com$", "");
        final String[] params = jmxParams;
        final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, tabName);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            LastTestStorage.setLastTestStorageOrs(host, tabName, params);
            TestRunner.runTest(project, host, params,
                    new OrsRunStrategy(new OSCommands(host), host, Locations.getLocalResultFile()),
                    uiDescriptor);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(ProjectViewRunGroup.getTarget(e) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
