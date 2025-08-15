package com.workday.plugin.testrunner.actions;

import static com.intellij.icons.AllIcons.RunConfigurations.TestState.Run;
import static com.workday.plugin.testrunner.actions.ReRunLastTestAction.showBalloon;
import static com.workday.plugin.testrunner.common.Locations.LOCALHOST;
import static com.workday.plugin.testrunner.common.Locations.SUV_RESULTS_FILE;
import static com.workday.plugin.testrunner.common.Locations.TEST_RESULTS_FOLDER_SUV_DOCKER;
import static com.workday.plugin.testrunner.common.Locations.getBasePath;
import static com.workday.plugin.testrunner.common.Locations.getLocalResultFile;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

import com.workday.plugin.testrunner.common.HostPromptDialog;
import com.workday.plugin.testrunner.common.LastTestStorage;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.execution.LocalRunStrategy;
import com.workday.plugin.testrunner.execution.OSCommands;
import com.workday.plugin.testrunner.execution.ParamBuilder;
import com.workday.plugin.testrunner.execution.RemoteRunStrategy;
import com.workday.plugin.testrunner.execution.RunStrategy;
import com.workday.plugin.testrunner.execution.TestRunner;
import com.workday.plugin.testrunner.target.TestTargetExtractor;
import com.workday.plugin.testrunner.ui.UiContentDescriptor;

/**
 * This class contributes gutter markers for OMS test methods and classes.
 * It provides actions to run tests locally or remotely based on the context of the PsiElement.
 *
 * @author alexander.aizikivsky
 * @since Jun-2025
 */
public class GutterMarkerContributor
    extends RunLineMarkerContributor {

    @Override
    public Info getInfo(@NotNull PsiElement element) {
        if (!(element instanceof PsiIdentifier)) {
            return null;
        }

        PsiElement parent = element.getParent();
        if (parent instanceof PsiMethod) {
            return getMethodInfo((PsiMethod) parent, element.getProject());
        }
        else if (parent instanceof PsiClass) {
            return getClassInfo((PsiClass) parent, element.getProject());
        }
        else {
            return null;
        }
    }

    private Info getMethodInfo(PsiMethod method, Project project) {
        Locations.setBasePath(project.getBasePath());
        final boolean omsTestMethod = TestTargetExtractor.isTestLikeMethod(method);
        if (!omsTestMethod) {
            return null;
        }

        PsiClass clazz = method.getContainingClass();

        if (clazz == null) {
            return null;
        }
        final boolean omsTestClass = TestTargetExtractor.isOmsTestClass(clazz);
        if (!omsTestClass) {
            return null;
        }

        String classFqName = clazz.getQualifiedName();
        if (classFqName == null) {
            return null;
        }

        String methodSignature = buildMethodSignature(classFqName, method);
        final String[] methodArgs = ParamBuilder.getMethodArgs(methodSignature);
        AnAction runLocal = createAction("Run '" + method.getName() + "' on local OMS", project, methodArgs, false);
        AnAction runRemote = createAction("Run '" + method.getName() + "' on SUV", project, methodArgs, true);

        return new Info(Run, element -> "Run OMS Test Method", runLocal, runRemote);
    }

    private Info getClassInfo(PsiClass clazz, Project project) {
        Locations.setBasePath(project.getBasePath());
        if (!TestTargetExtractor.isOmsTestClass(clazz)) {
            return null;
        }

        String fqName = clazz.getQualifiedName();
        if (fqName == null) {
            return null;
        }

        final String[] classArgs = ParamBuilder.getClassArgs(fqName);
        AnAction runLocal = createAction("Run '" + clazz.getName() + "' on local OMS", project, classArgs, false);
        AnAction runRemote = createAction("Run '" + clazz.getName() + "' on SUV", project, classArgs, true);

        return new Info(Run, element -> "Run OMS Test Class", runLocal, runRemote);
    }

    private @NotNull AnAction createAction(
        final String runTabName, final Project project, final String[] jmxParameters, final boolean isRemote) {

        return new AnAction(runTabName, null, Run) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {

                final RunStrategy runStrategy;
                final String host;
                if (isRemote) {
                    HostPromptDialog dialog = new HostPromptDialog();
                    if (!dialog.showAndGet()) {
                        // canceled by user
                        return;
                    }
                    host = dialog.getHost();
                    if (host.isBlank()) {
                        showBalloon(project, "No valid host specified", NotificationType.ERROR);
                        return;
                    }
                    runStrategy = new RemoteRunStrategy(new OSCommands(host), host, getLocalResultFile(),
                        SUV_RESULTS_FILE, TEST_RESULTS_FOLDER_SUV_DOCKER);
                }
                else {
                    host = LOCALHOST;
                    runStrategy = new LocalRunStrategy(new OSCommands(host), getLocalResultFile(), getBasePath());
                }
                final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, runTabName);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    LastTestStorage.setLastTestStorage(host, isRemote, runTabName, jmxParameters);
                    TestRunner.runTest(project, host, jmxParameters, runStrategy, uiDescriptor);
                });
            }
        };
    }

    private String buildMethodSignature(String classFqName, PsiMethod method) {
        StringBuilder paramTypes = new StringBuilder();
        PsiParameter[] parameters = method.getParameterList().getParameters();
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                paramTypes.append(",");
            }
            paramTypes.append(parameters[i].getType().getCanonicalText());
        }

        return paramTypes.isEmpty()
            ? classFqName + "@" + method.getName()
            : classFqName + "@" + method.getName() + "(" + paramTypes + ")";
    }

}