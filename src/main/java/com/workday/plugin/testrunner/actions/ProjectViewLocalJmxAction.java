package com.workday.plugin.testrunner.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import com.workday.plugin.testrunner.common.LastTestStorage;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.execution.LocalRunStrategy;
import com.workday.plugin.testrunner.execution.OSCommands;
import com.workday.plugin.testrunner.execution.ParamBuilder;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

public class ProjectViewLocalJmxAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        ProjectViewRunGroup.OmsTarget target = ProjectViewRunGroup.getTarget(e);
        if (target == null) return;

        String[] jmxParams;
        if (target.isPackage()) {
            String category = ProjectViewRunGroup.promptCategory(project);
            if (category == null) return;
            jmxParams = ParamBuilder.getPackageArgs(target.packageName(), category);
        } else {
            jmxParams = ParamBuilder.getClassArgs(target.classFqn());
        }

        Locations.setBasePath(project.getBasePath());
        final String tabName = target.shortName() + "@localJmx";
        final String[] params = jmxParams;
        final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, tabName);
        LastTestStorage.setLastTestStorageLocalJmx(tabName, params);
        LocalRunStrategy strategy = new LocalRunStrategy(
                new OSCommands(Locations.LOCALHOST), Locations.getLocalResultFile(), Locations.getBasePath());
        strategy.setProcessHandler(uiDescriptor.getUiProcessHandler());
        strategy.runJmxTest(params);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
