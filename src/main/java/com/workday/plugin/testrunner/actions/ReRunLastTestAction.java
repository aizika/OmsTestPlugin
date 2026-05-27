package com.workday.plugin.testrunner.actions;

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
import com.workday.plugin.testrunner.execution.OrsRunStrategy;
import com.workday.plugin.testrunner.execution.ParamBuilder;
import com.workday.plugin.testrunner.execution.RemoteJRunStrategy;
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
        super("Rerun", "Rerun last test",
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
        final boolean isRemoteJ = lastEntry.isRemoteJ();
        final boolean isOrs = lastEntry.isOrs();
        final boolean isLocalJmx = lastEntry.isLocalJmx();
        String[] jmxParameters = lastEntry.getJmxParameters();
        String runTabName = lastEntry.getRunTabName();
        String host = lastEntry.getHost();

        Locations.setBasePath(basePath);
        final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, runTabName);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            if (jmxParameters == null || jmxParameters.length == 0) {
                return;
            }

            // RemoteJ: run via Gradle remoteServerTest
            if (isRemoteJ) {
                final RemoteJRunStrategy strategy = new RemoteJRunStrategy();
                strategy.setProcessHandler(uiDescriptor.getUiProcessHandler());
                strategy.runGradleTest(ParamBuilder.getGradleTestArg(jmxParameters));
                return;
            }

            // Local JMX: run via direct JMX connection to local OTS
            if (isLocalJmx) {
                final RunStrategy runStrategy = new LocalRunStrategy(
                        new OSCommands(Locations.LOCALHOST), getLocalResultFile(), Locations.getBasePath());
                TestRunner.runTest(project, Locations.LOCALHOST, jmxParameters, runStrategy, uiDescriptor);
                return;
            }

            // ORS: run via SSH + jmxterm in ORS PID namespace
            if (!isOrs) {
                showBalloon(project, "Cannot re-run: no stored configuration");
                return;
            }
            if (host == null || host.isBlank()) {
                showBalloon(project, "Host is not specified");
                return;
            }
            final SshProbe.Result probe = SshProbe.probe(host);
            if (probe.exitCode != 0) {
                showBalloon(project, "Cannot use host: " + host + ": " + probe.reason);
                return;
            }
            final RunStrategy runStrategy = new OrsRunStrategy(new OSCommands(host), host, getLocalResultFile());
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