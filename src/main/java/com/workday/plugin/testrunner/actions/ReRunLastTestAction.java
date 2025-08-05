package com.workday.plugin.testrunner.actions;

import static com.workday.plugin.testrunner.common.Locations.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import com.workday.plugin.testrunner.common.LastTestStorage;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.execution.LocalRunStrategy;
import com.workday.plugin.testrunner.execution.OSCommands;
import com.workday.plugin.testrunner.execution.RunStrategy;
import com.workday.plugin.testrunner.execution.RemoteRunStrategy;
import com.workday.plugin.testrunner.execution.TestRunner;

/**
 * Action to re-run the last executed OMS test.
 * It retrieves the last test parameters and executes it again using the appropriate run strategy.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class ReRunLastTestAction
    extends AnAction {

    public ReRunLastTestAction() {
        super("Re-Run OMS Test", "Re-runs the last executed OMS test", AllIcons.Actions.Restart);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Locations.setBasePath(project.getBasePath());

        final boolean isRemote = LastTestStorage.getIsRemote();
        String[] jmxParameters = LastTestStorage.getJmxParameters();
        String runTabName = LastTestStorage.getRunTabName();
        String host;

        RunStrategy runStrategy;
        if (isRemote) {
            host = LastTestStorage.getHost();
            runStrategy = new RemoteRunStrategy(new OSCommands(host), host, getLocalResultFile(),
                SUV_RESULTS_FILE, TEST_RESULTS_FOLDER_SUV_DOCKER);
        }
        else {
            host = LOCALHOST;
            runStrategy = new LocalRunStrategy(new OSCommands(host), getLocalResultFile(), getBasePath());
        }

        if (host == null || host.isBlank()) {
            return;
        }
        if (jmxParameters == null || jmxParameters.length == 0) {
            return;
        }
        if (runTabName == null || runTabName.isBlank()) {
            runTabName = "Re-run Last Test";
        }

        TestRunner.runTest(project, host, runTabName, jmxParameters, runStrategy);
    }
}