package com.workday.plugin.testrunner.actions;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.sm.runner.SMTRunnerNodeDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.UIUtil;

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
 * Toolbar action that runs the currently selected test tree node using the same
 * strategy as the last executed test (ORS, Local JMX, or RemoteJ).
 *
 * @author alexander.aizikivsky
 * @since May-2026
 */
public class RunSelectedInRemoteJAction extends AnAction {

    private final JComponent viewComp;
    private final Project project;

    public RunSelectedInRemoteJAction(JComponent viewComp, Project project) {
        super("Run", "Run selected test", AllIcons.Actions.Execute);
        this.viewComp = viewComp;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        AbstractTestProxy proxy = getSelectedProxy();
        if (proxy == null) return;

        TestLocation loc = parseLocation(proxy);
        if (loc == null) return;

        LastTestStorage.LastTestEntry lastEntry = LastTestStorage.getLastTestEntry();

        UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, loc.tabName);
        UiContentDescriptor.UiProcessHandler handler = uiDescriptor.getUiProcessHandler();

        if (lastEntry == null || lastEntry.isRemoteJ()) {
            RemoteJRunStrategy strategy = new RemoteJRunStrategy();
            strategy.setProcessHandler(handler);
            strategy.runGradleTest(loc.gradleArg);

        } else if (lastEntry.isLocalJmx()) {
            Locations.setBasePath(lastEntry.getBasePath());
            String[] jmxParams = loc.toJmxArgs();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                RunStrategy runStrategy = new LocalRunStrategy(
                        new OSCommands(Locations.LOCALHOST), Locations.getLocalResultFile(), Locations.getBasePath());
                TestRunner.runTest(project, Locations.LOCALHOST, jmxParams, runStrategy, uiDescriptor);
            });

        } else if (lastEntry.isOrs()) {
            String host = lastEntry.getHost();
            if (host == null || host.isBlank()) {
                ReRunLastTestAction.showBalloon(project, "Host is not specified");
                return;
            }
            Locations.setBasePath(lastEntry.getBasePath());
            String[] jmxParams = loc.toJmxArgs();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                SshProbe.Result probe = SshProbe.probe(host);
                if (probe.exitCode != 0) {
                    ReRunLastTestAction.showBalloon(project, "Cannot use host: " + host + ": " + probe.reason);
                    return;
                }
                RunStrategy runStrategy = new OrsRunStrategy(
                        new OSCommands(host), host, Locations.getLocalResultFile());
                TestRunner.runTest(project, host, jmxParams, runStrategy, uiDescriptor);
            });
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        AbstractTestProxy proxy = getSelectedProxy();
        e.getPresentation().setEnabled(proxy != null);
        if (proxy != null) {
            LastTestStorage.LastTestEntry lastEntry = LastTestStorage.getLastTestEntry();
            String strategy = lastEntry == null || lastEntry.isRemoteJ() ? "RemoteJ"
                    : lastEntry.isLocalJmx() ? "Local JMX"
                    : "SUV JMX";
            e.getPresentation().setDescription("Run selected test via " + strategy);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Nullable
    private AbstractTestProxy getSelectedProxy() {
        JTree tree = UIUtil.findComponentOfType(viewComp, JTree.class);
        if (tree == null) return null;

        Object last = tree.getLastSelectedPathComponent();
        if (!(last instanceof DefaultMutableTreeNode node)) return null;
        if (!(node.getUserObject() instanceof SMTRunnerNodeDescriptor descriptor)) return null;

        AbstractTestProxy proxy = descriptor.getElement();
        if (proxy == null) return null;

        String url = proxy.getLocationUrl();
        if (url == null || !url.startsWith("java:")) return null;

        return proxy;
    }

    @Nullable
    private TestLocation parseLocation(AbstractTestProxy proxy) {
        String javaPath = proxy.getLocationUrl().substring("java:".length());
        String[] parts = javaPath.split("#", 2);
        String className = parts[0];
        if (className.isBlank()) return null;

        String shortClass = className.substring(className.lastIndexOf('.') + 1);

        if (parts.length < 2 || parts[1].isBlank()) {
            return new TestLocation(className, null, className, shortClass + "@run");
        }

        String methodName = parts[1];
        return new TestLocation(className, methodName,
                className + "." + methodName, methodName + "@run");
    }

    private record TestLocation(String className, @Nullable String methodName,
                                String gradleArg, String tabName) {
        String[] toJmxArgs() {
            return methodName != null
                    ? ParamBuilder.getMethodArgs(className + "@" + methodName)
                    : ParamBuilder.getClassArgs(className);
        }
    }
}
