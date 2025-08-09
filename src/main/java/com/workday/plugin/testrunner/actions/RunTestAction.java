package com.workday.plugin.testrunner.actions;

import static com.workday.plugin.testrunner.common.Locations.*;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import com.workday.plugin.testrunner.common.HostPromptDialog;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.common.TargetType;
import com.workday.plugin.testrunner.common.TestEnvironment;
import com.workday.plugin.testrunner.execution.LocalRunStrategy;
import com.workday.plugin.testrunner.execution.OSCommands;
import com.workday.plugin.testrunner.execution.ParamBuilder;
import com.workday.plugin.testrunner.execution.RunStrategy;
import com.workday.plugin.testrunner.execution.RemoteRunStrategy;
import com.workday.plugin.testrunner.execution.TestRunner;
import com.workday.plugin.testrunner.target.TargetVerifier;
import com.workday.plugin.testrunner.target.TestTarget;
import com.workday.plugin.testrunner.target.TestTargetExtractor;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * Currently is not used, but can be useful for the context menu actions.
 *
 * Action to run OMS tests based on the selected target type (method or class).
 * It prompts for a host and executes the test using the appropriate run strategy.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class RunTestAction
    extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null || getHost().isBlank()) return;
        Locations.setBasePath(project.getBasePath());

        String host = getHost();
        if (host.isBlank()) return;

        TestTarget testTarget = TestTargetExtractor.getTestTarget(e);
        RunStrategy runStrategy = getRunStrategy(host);
         final String[] jmxParams = ParamBuilder.getJmxParams(getTargetType(), testTarget);
        TestRunner.runTest(project, host, jmxParams, runStrategy, null);
    }


    private static void callTestRunner(final Project project,
                           final String host,
                           final RunStrategy runStrategy,
                           final String[] jmxParams) {
        final UiContentDescriptor.UiProcessHandler processHandler = new UiContentDescriptor.UiProcessHandler();
        final UiContentDescriptor descriptor = UiContentDescriptor.createDescriptor(project, "HelloWorld", processHandler);
        final ConsoleView console = (ConsoleView) descriptor.getExecutionConsole();
        RunContentManager.getInstance(project)
            .showRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);

        try {
            processHandler.startNotify();
            console.print("Running test on " + host + "\n", ConsoleViewContentType.SYSTEM_OUTPUT);

            final int jmxPort = runStrategy.getOmsJmxPort();
            runStrategy.deleteTempFiles();
            runStrategy.verifyOms();
            runStrategy.maybeStartPortForwarding(jmxPort);

            new TestRunner(runStrategy, jmxPort, jmxParams).runTests(processHandler);
        }
        catch (Exception ex) {
            if (descriptor != null && descriptor.getUiProcessHandler() != null) {
                descriptor.getUiProcessHandler().notifyTextAvailable(
                    "An error occurred: " + ex.getMessage() + "\n",
                    ProcessOutputTypes.STDERR
                                                                    );
                descriptor.getUiProcessHandler().destroyProcess();
                RunContentManager.getInstance(project)
                    .removeRunContent(DefaultRunExecutor.getRunExecutorInstance(), descriptor);
            }
        }
    }

    private static void verifyTarget(TestTarget target, TargetType type) {
        TargetVerifier.verifyTarget(target, type);
    }

    protected String getHost() {
        HostPromptDialog dialog = new HostPromptDialog();
        if (dialog.showAndGet()) return dialog.getHost();
        return "";
    }

    protected @NotNull TestEnvironment getTestEnvironment() {
        return TestEnvironment.REMOTE;
    }

    protected @NotNull TargetType getTargetType() {
        return TargetType.METHOD;
    }

    protected RunStrategy getRunStrategy(final String host) {
        if (getTestEnvironment().equals(TestEnvironment.LOCAL)) {
            return new LocalRunStrategy(new OSCommands(host), getLocalResultFile(), getBasePath());
        } else {
            return new RemoteRunStrategy(new OSCommands(host), host, getLocalResultFile(), SUV_RESULTS_FILE, TEST_RESULTS_FOLDER_SUV_DOCKER);
        }
    }

    public static class RemoteMethod
        extends RunTestAction {

        @Override
        protected @NotNull TestEnvironment getTestEnvironment() {
            return TestEnvironment.REMOTE;
        }

        @Override
        protected RunStrategy getRunStrategy(final String host) {
            return new RemoteRunStrategy(new OSCommands(host), host, getLocalResultFile(), SUV_RESULTS_FILE, TEST_RESULTS_FOLDER_SUV_DOCKER);
        }

        @Override
        protected @NotNull TargetType getTargetType() {
            return TargetType.METHOD;
        }

        @Override
        protected String getHost() {
            return super.getHost();
        }
    }

    static class RemoteClass
        extends RunTestAction {

        @Override
        protected @NotNull TestEnvironment getTestEnvironment() {
            return TestEnvironment.REMOTE;
        }

        @Override
        protected @NotNull TargetType getTargetType() {
            return TargetType.CLASS;
        }

        @Override
        protected String getHost() {
            return super.getHost();
        }
        protected RunStrategy getRunStrategy(final String host) {
            return new RemoteRunStrategy(new OSCommands(host), host, getLocalResultFile(), SUV_RESULTS_FILE, TEST_RESULTS_FOLDER_SUV_DOCKER);
        }
    }

    static class LocalMethod
        extends RunTestAction {

        @Override
        protected @NotNull TestEnvironment getTestEnvironment() {
            return TestEnvironment.LOCAL;
        }

        @Override
        protected @NotNull TargetType getTargetType() {
            return TargetType.METHOD;
        }

        @Override
        protected String getHost() {
            return LOCALHOST;
        }
        protected RunStrategy getRunStrategy(final String host) {
            return new LocalRunStrategy(new OSCommands(host), getLocalResultFile(), getBasePath());
        }
    }

    static class LocalClass
        extends RunTestAction {

        @Override
        protected @NotNull TestEnvironment getTestEnvironment() {
            return TestEnvironment.LOCAL;
        }

        @Override
        protected @NotNull TargetType getTargetType() {
            return TargetType.CLASS;
        }

        @Override
        protected String getHost() {
            return LOCALHOST;
        }

        protected RunStrategy getRunStrategy(final String host) {
            return new LocalRunStrategy(new OSCommands(host), getLocalResultFile(), getBasePath());
        }
    }
}