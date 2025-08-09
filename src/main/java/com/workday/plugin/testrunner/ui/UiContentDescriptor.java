package com.workday.plugin.testrunner.ui;

import java.awt.*;
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
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ui.UIUtil;

import com.workday.plugin.testrunner.actions.ReRunLastTestAction;

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

        JPanel consolePanel = createJunitPanel(consoleView);
        return new UiContentDescriptor(consoleView, processHandler, consolePanel, runTabName);
    }

    private static void installTestTreeNavigation(ConsoleView consoleView, Project project) {
        JTree tree = UIUtil.findComponentOfType(consoleView.getComponent(), JTree.class);
        if (tree == null) {
            return;
        }

        tree.addTreeSelectionListener(event -> {
            Object lastPathComponent = event.getPath().getLastPathComponent();
            if (!(lastPathComponent instanceof DefaultMutableTreeNode selectedNode)) {
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

            String path = locationUrl.substring("java:".length());
            String[] parts = path.split("#");
            String classFqName = parts[0];
            String methodName = parts.length > 1 ? parts[1] : null;

            JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
            PsiClass psiClass = psiFacade.findClass(classFqName, GlobalSearchScope.allScope(project));
            if (psiClass == null) {
                return;
            }

            PsiElement psi;
            if (methodName != null) {
                PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
                psi = methods.length > 0 ? methods[0] : psiClass;
            }
            else {
                psi = psiClass;
            }

            if (psi instanceof Navigatable navigatable && navigatable.canNavigate()) {
                navigatable.navigate(true);
            }
        });
    }

    public static ConsoleView createConsoleView(Project project, ProcessHandler handler) {
        ParsedResultRunConfiguration config = new ParsedResultRunConfiguration(project);
        SMTRunnerConsoleProperties consoleProperties = new SMTRunnerConsoleProperties(config, "ParsedResults",
            DefaultRunExecutor.getRunExecutorInstance()) {
            @Override
            public SMTestLocator getTestLocator() {
                return NoOpTestLocator.INSTANCE;
            }
        };

        ConsoleView view = SMTestRunnerConnectionUtil.createConsole("ParsedResults", consoleProperties);
        view.attachToProcess(handler);
        return view;
    }

    public static JPanel createJunitPanel(ConsoleView consoleView) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new ReRunLastTestAction());
        JComponent toolbar = ActionManager.getInstance().createActionToolbar("GradleTestToolbar", actionGroup,
            false).getComponent();

        JPanel consolePanel = new JPanel(new BorderLayout());
        consolePanel.add(toolbar, BorderLayout.WEST);
        consolePanel.add(consoleView.getComponent(), BorderLayout.CENTER);
        return consolePanel;
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

        public void finish(final int exitCode) {
            notifyProcessTerminated(exitCode);
        }

        @Override
        protected void destroyProcessImpl() {
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
    }
}