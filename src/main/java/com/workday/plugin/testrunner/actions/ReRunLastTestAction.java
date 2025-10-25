package com.workday.plugin.testrunner.actions;

import static com.workday.plugin.testrunner.common.Locations.SUV_RESULTS_FILE;
import static com.workday.plugin.testrunner.common.Locations.TEST_RESULTS_FOLDER_SUV_DOCKER;
import static com.workday.plugin.testrunner.common.Locations.getBasePath;
import static com.workday.plugin.testrunner.common.Locations.getLocalResultFile;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import com.workday.plugin.testrunner.common.LastTestStorage;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.common.SshProbe;
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

    @SuppressWarnings("ActionPresentationInstantiatedInCtor")  // this is the only way to show the correct icon
    public ReRunLastTestAction() {
        super("Re-Run OMS Test", "Re-runs the last executed OMS test",
            IconLoader.getIcon("icons/rerun.svg", ReRunLastTestAction.class));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {

        Project project = e.getProject();
        if (project == null) {
            return;
        }

        if (!LastTestStorage.isLastTestStored()) {
            showBalloon(project, "No last test stored");
            return;
        }

        RunContentDescriptor descriptor = RunContentManager.getInstance(project)
            .getSelectedContent();
        String tabKey = descriptor != null ? descriptor.getDisplayName() : null;
        LastTestStorage.LastTestEntry lastEntry;
        if (tabKey != null) {
            lastEntry = LastTestStorage.getLastEntry(tabKey);
            if (lastEntry == null) {
                lastEntry = LastTestStorage.getLastTestEntry();
            }
        }
        else {
            lastEntry = LastTestStorage.getLastTestEntry();
        }

        if (lastEntry == null) {
            showBalloon(project, "No last test stored");
            return;
        }

        final String basePath = lastEntry.getBasePath();
        final boolean isRemote = lastEntry.isRemote();
        String[] jmxParameters = lastEntry.getJmxParameters();
        String runTabName = lastEntry.getRunTabName();
        String host = lastEntry.getHost();

        Locations.setBasePath(basePath);
        final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, runTabName);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            final RunStrategy runStrategy;
            if (isRemote) {
                if (host == null || host.isBlank()) {
                    showBalloon(project, "Host is not specified");
                    return;
                }
                final SshProbe.Result probe = SshProbe.probe(host);

                if (probe.exitCode != 0) {
                    showBalloon(project, "Cannot use host: " + host + ": " + probe.reason);
                    return;
                }
                runStrategy = new RemoteRunStrategy(new OSCommands(host), host, getLocalResultFile(),
                    SUV_RESULTS_FILE, TEST_RESULTS_FOLDER_SUV_DOCKER);
            }
            else {
                runStrategy = new LocalRunStrategy(new OSCommands(host), getLocalResultFile(), getBasePath());
            }

            if (jmxParameters == null || jmxParameters.length == 0) {
                return;
            }
            TestRunner.runTest(project, host, jmxParameters, runStrategy, uiDescriptor);
        });
    }

    public static void showBalloon(Project project, String message) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("OmsTest Notifications") // must match the ID you registered in plugin.xml
            .createNotification(message, NotificationType.ERROR)
            .notify(project);
    }
}