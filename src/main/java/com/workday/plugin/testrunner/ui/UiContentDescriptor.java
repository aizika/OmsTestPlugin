package com.workday.plugin.testrunner.ui;

import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.Executor;
import com.intellij.execution.Location;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTRunnerNodeDescriptor;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.UIUtil;

import com.workday.plugin.testrunner.actions.ReRunLastTestAction;
import com.workday.plugin.testrunner.actions.RunSelectedInRemoteJAction;

/**
 * This class represents a UI content descriptor for JUnit test results in Run Tool Window.
 *         +--------------------------------------------------------------------------------------+
 *         |                       Run Tool Window Tab ("OMS Test Results")                       |
 *         |                        [Owned by RunContentDescriptor]                               |
 *         +--------------------------------------------------------------------------------------+
 *         |  Toolbar (left top corner)                                                           |
 *         |                                                                                      |
 *         |  +--------------------------------+  +----------------------------------------+      |
 *         |  |   Test Tree Panel              |  |        Console Output Panel            |      |
 *         |  |  [TestResultsViewer]           |  |  [ConsoleView inside SMTRunnerConsoleView]    |
 *         |  |  - Based on SMTestProxy tree   |  |  - Receives process output, SM logs    |      |
 *         |  |  - Updates via SM events       |  |  - Printed system output, test logs    |      |
 *         |  +--------------------------------+  +----------------------------------------+      |
 *         |      ▲                                                                         ▲     |
 *         |      |                                                                         |     |
 *         |      |        [SMTRunnerConsoleView manages both left and right panes]         |     |
 *         +--------------------------------------------------------------------------------------+
 *
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class UiContentDescriptor
    extends RunContentDescriptor {

    public UiContentDescriptor(ConsoleView consoleView,
                               UiProcessHandler processHandler,
                               JComponent component,
                               String displayName) {
        super(consoleView, processHandler, component, displayName);
    }

    public static UiContentDescriptor createDescriptor(Project project, String runTabName,
                                                       final UiProcessHandler processHandler) {
        ConsoleView consoleView = createConsoleView(project, processHandler);
        installTestTreeNavigation(consoleView, project);

        return new UiContentDescriptor(consoleView, processHandler, consoleView.getComponent(), runTabName);
    }

    private static void installTestTreeNavigation(ConsoleView consoleView, Project project) {
        JTree tree = UIUtil.findComponentOfType(consoleView.getComponent(), JTree.class);
        if (tree == null) {
            return;
        }

        tree.addTreeSelectionListener(event -> navigateFromSelectedNode(tree, project));
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getButton() == java.awt.event.MouseEvent.BUTTON1) {
                    int row = tree.getRowForLocation(e.getX(), e.getY());
                    javax.swing.tree.TreePath path = (row >= 0) ? tree.getPathForRow(row) : null;
                    if (path != null && tree.isPathSelected(path)) {
                        navigateFromSelectedNode(tree, project);
                    }
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                handlePopup(e);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                handlePopup(e);
            }

            private void handlePopup(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = tree.getRowForLocation(e.getX(), e.getY());
                if (row < 0) return;
                tree.setSelectionRow(row);
                AbstractTestProxy proxy = getProxyAtRow(tree, row);
                if (proxy == null || RunSelectedInRemoteJAction.isVariantLeaf(proxy)) return;
                String url = proxy.getLocationUrl();
                if (url == null || (!url.startsWith("java:") && !url.startsWith("pkg:"))) return;
                showRunPopup(tree, proxy, project, e);
            }
        });
    }

    private static AbstractTestProxy getProxyAtRow(JTree tree, int row) {
        javax.swing.tree.TreePath path = tree.getPathForRow(row);
        if (path == null) return null;
        Object last = path.getLastPathComponent();
        if (!(last instanceof DefaultMutableTreeNode node)) return null;
        if (!(node.getUserObject() instanceof SMTRunnerNodeDescriptor descriptor)) return null;
        return descriptor.getElement();
    }

    private static void showRunPopup(JTree tree, AbstractTestProxy proxy, Project project,
                                     java.awt.event.MouseEvent e) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem item = new JMenuItem("Run selected", AllIcons.Actions.Execute);
        item.addActionListener(ev -> RunSelectedInRemoteJAction.execute(proxy, project));
        popup.add(item);
        popup.show(tree, e.getX(), e.getY());
    }

    private static void navigateFromSelectedNode(JTree tree, Project project) {
        Object last = tree.getLastSelectedPathComponent();
        if (!(last instanceof DefaultMutableTreeNode selectedNode)) {
            return;
        }

        Object userObject = selectedNode.getUserObject();
        if (!(userObject instanceof SMTRunnerNodeDescriptor descriptor)) {
            return;
        }

        AbstractTestProxy proxy = descriptor.getElement();
        if (proxy == null) {
            return;
        }

        String locationUrl = proxy.getLocationUrl();
        if (locationUrl == null || !locationUrl.startsWith("java:")) {
            return;
        }

        String[] parts = locationUrl.substring("java:".length()).split("#");
        PsiClass psiClass = JavaPsiFacade.getInstance(project)
            .findClass(parts[0], GlobalSearchScope.allScope(project));
        if (psiClass == null) {
            return;
        }

        PsiElement target = (parts.length > 1)
            ? psiClass.findMethodsByName(parts[1], false)[0]
            : psiClass;

        if (target instanceof Navigatable navigatable && navigatable.canNavigate()) {
            navigatable.navigate(true);
        }
    }

    public static ConsoleView createConsoleView(Project project, UiProcessHandler processHandler) {
        ParsedResultRunConfiguration config = new ParsedResultRunConfiguration(project);
        SMTRunnerConsoleProperties consoleProperties = new SMTRunnerConsoleProperties(config, "ParsedResults",
            DefaultRunExecutor.getRunExecutorInstance()) {
            @Override
            public SMTestLocator getTestLocator() {
                return NoOpTestLocator.INSTANCE;
            }
        };

        ConsoleView view = SMTestRunnerConnectionUtil.createConsole("ParsedResults", consoleProperties);
        view.attachToProcess(processHandler);
        injectActionsIntoToolbar(view.getComponent(), processHandler, project);
        return view;
    }

    private static void injectActionsIntoToolbar(JComponent viewComp, UiProcessHandler processHandler, Project project) {
        ActionToolbarImpl toolbar = UIUtil.findComponentOfType(viewComp, ActionToolbarImpl.class);
        if (toolbar == null || !(toolbar.getActionGroup() instanceof DefaultActionGroup dag)) {
            return;
        }
        dag.addSeparator();
        dag.add(new AnAction("Stop", "Stop the running test", AllIcons.Actions.Suspend) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                processHandler.destroyProcess();
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                e.getPresentation().setEnabled(!processHandler.isProcessTerminated());
            }

            @Override
            public @NotNull ActionUpdateThread getActionUpdateThread() {
                return ActionUpdateThread.EDT;
            }
        });
        dag.add(new ReRunLastTestAction());
        dag.add(new RunSelectedInRemoteJAction(viewComp, project));
    }

    public static UiContentDescriptor createUiDescriptor(final Project project, final String runTabName) {
        final UiProcessHandler processHandler = new UiProcessHandler();
        final UiContentDescriptor descriptor = createDescriptor(
            project, runTabName,
            processHandler);
        RunContentManager.getInstance(project)
            .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
        processHandler.startNotify();
        return descriptor;
    }

    public UiProcessHandler getUiProcessHandler() {
        return (UiProcessHandler) getProcessHandler();
    }

    public static class ParsedResultRunConfiguration
        extends RunConfigurationBase<RunProfileState> {

        public ParsedResultRunConfiguration(Project project) {
            super(project, ParsedResultConfigFactory.INSTANCE, "ParsedRunConfig");
        }

        @Override
        public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
            return null;
        }

        @Override
        public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
            return new SettingsEditor<>() {
                @Override
                protected void resetEditorFrom(@NotNull RunConfiguration s) {
                }

                @Override
                protected void applyEditorTo(@NotNull RunConfiguration s) {
                }

                @Override
                protected @NotNull JComponent createEditor() {
                    return new JPanel();
                }
            };
        }
    }

    public static class ParsedResultConfigFactory
        extends ConfigurationFactory {

        public static final ParsedResultConfigFactory INSTANCE = new ParsedResultConfigFactory();

        public ParsedResultConfigFactory() {
            super(ParsedResultConfigType.INSTANCE);
        }

        @Override
        public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new ParsedResultRunConfiguration(project);
        }
    }

    public static class ParsedResultConfigType
        implements ConfigurationType {

        public static final ParsedResultConfigType INSTANCE = new ParsedResultConfigType();

        @Override
        public @NotNull String getDisplayName() {
            return "Parsed Test Viewer";
        }

        @Override
        public String getConfigurationTypeDescription() {
            return "Displays parsed JUnit results";
        }

        @Override
        public Icon getIcon() {
            return AllIcons.General.Information;
        }

        @Override
        public @NotNull String getId() {
            return "PARSED_TEST_RESULT_VIEWER";
        }

        @Override
        public ConfigurationFactory[] getConfigurationFactories() {
            return new ConfigurationFactory[] { ParsedResultConfigFactory.INSTANCE };
        }
    }

    private enum NoOpTestLocator
        implements SMTestLocator {
        INSTANCE;

        @Override
        public @NotNull List<Location> getLocation(@NotNull String protocol,
                                                   @NotNull String path,
                                                   @NotNull Project project,
                                                   @NotNull GlobalSearchScope scope) {
            return Collections.emptyList();
        }
    }

    public static class UiProcessHandler
        extends ProcessHandler {

        private volatile Runnable cancelHandler;

        public void setCancelHandler(final Runnable cancelHandler) {
            this.cancelHandler = cancelHandler;
        }

        public void finish(final int exitCode) {
            notifyProcessTerminated(exitCode);
        }

        @Override
        protected void destroyProcessImpl() {
            final Runnable h = this.cancelHandler;
            if (h != null) {
                h.run();
            }
            notifyProcessTerminated(0);
        }

        @Override
        protected void detachProcessImpl() {
            notifyProcessDetached();
        }

        @Override
        public boolean detachIsDefault() {
            return false;
        }

        @Override
        public OutputStream getProcessInput() {
            return null;
        }

        public void log(String message) {
            notifyTextAvailable(message + "\n", ProcessOutputType.STDOUT);
        }

        public void error(String message) {
            notifyTextAvailable(message + "\n", ProcessOutputType.STDERR);
        }
    }
}