package com.workday.plugin.testrunner.actions;

import static com.workday.plugin.testrunner.common.Locations.LOCALHOST;
import static com.workday.plugin.testrunner.common.Locations.SUV_RESULTS_FILE;
import static com.workday.plugin.testrunner.common.Locations.TEST_RESULTS_FOLDER_SUV_DOCKER;
import static com.workday.plugin.testrunner.common.Locations.getBasePath;
import static com.workday.plugin.testrunner.common.Locations.getLocalResultFile;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import com.workday.plugin.testrunner.common.LastTestStorage;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.execution.LocalRunStrategy;
import com.workday.plugin.testrunner.execution.OSCommands;
import com.workday.plugin.testrunner.execution.RemoteRunStrategy;
import com.workday.plugin.testrunner.execution.RunStrategy;
import com.workday.plugin.testrunner.execution.TestRunner;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * Action to re-run the last executed OMS test.
 * It retrieves the last test parameters and executes it again using the appropriate run strategy.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class ReRunLastTestAction
    extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        Project project = e.getProject();
        if (project == null) {
            return;
        }

        final String basePath = LastTestStorage.getBasePath();
        final boolean isRemote = LastTestStorage.getIsRemote();
        String[] jmxParameters = LastTestStorage.getJmxParameters();
        String runTabName = LastTestStorage.getRunTabName();
        Locations.setBasePath(basePath);
        final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, runTabName);

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

        TestRunner.runTest(project, host, jmxParameters, runStrategy, uiDescriptor);
    }

}