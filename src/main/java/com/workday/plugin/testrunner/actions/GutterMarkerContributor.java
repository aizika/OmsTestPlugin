package com.workday.plugin.testrunner.actions;

import static com.intellij.icons.AllIcons.RunConfigurations.TestState.Run;
import static com.workday.plugin.testrunner.common.Locations.getLocalResultFile;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;

import com.workday.plugin.testrunner.actions.ReRunLastTestAction;
import com.workday.plugin.testrunner.common.HostPromptDialog;
import com.workday.plugin.testrunner.common.LastTestStorage;
import com.workday.plugin.testrunner.common.Locations;
import com.workday.plugin.testrunner.execution.LocalRunStrategy;
import com.workday.plugin.testrunner.execution.OSCommands;
import com.workday.plugin.testrunner.execution.OrsRunStrategy;
import com.workday.plugin.testrunner.execution.ParamBuilder;
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

        final String methodSignature = buildMethodSignature(classFqName, method);
        final String[] methodArgs = ParamBuilder.getMethodArgs(methodSignature);
        AnAction runSuvJmx = createOrsAction(method.getName(), project, methodArgs);
        AnAction runLocalJmx = createLocalJmxAction(method.getName(), project, methodArgs);

        return new Info(Run, element -> "Run OMS Test Method", runSuvJmx, runLocalJmx);
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
        AnAction runSuvJmx = createOrsAction(clazz.getName(), project, classArgs);
        AnAction runLocalJmx = createLocalJmxAction(clazz.getName(), project, classArgs);

        return new Info(Run, element -> "Run OMS Test Class", runSuvJmx, runLocalJmx);
    }

    private @NotNull AnAction createOrsAction(
            final String testName, final Project project, final String[] jmxParameters) {

        return new AnAction("Run " + testName + " (SUV JMX)", null, Run) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                HostPromptDialog dialog = new HostPromptDialog();
                if (!dialog.showAndGet()) {
                    return;
                }
                final String host = dialog.getHost();
                if (host.isBlank()) {
                    ReRunLastTestAction.showBalloon(project, "No valid host specified");
                    return;
                }
                final String runTabName = testName + "@ors:" + host.replaceFirst("\\.workdaysuv\\.com$", "");
                final RunStrategy runStrategy = new OrsRunStrategy(new OSCommands(host), host, getLocalResultFile());
                final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, runTabName);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    LastTestStorage.setLastTestStorageOrs(host, runTabName, jmxParameters);
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

    private @NotNull AnAction createLocalJmxAction(
            final String testName, final Project project, final String[] jmxParameters) {

        return new AnAction("Run " + testName + " (Local JMX)", null, Run) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent event) {
                final Project activeProject = event.getProject() != null ? event.getProject() : project;
                final String runTabName = testName + "@localJmx";
                final String localResultFile = Locations.getLocalResultFile();
                final String resultFolder = Locations.getBasePath();
                final RunStrategy runStrategy = new LocalRunStrategy(
                        new OSCommands(Locations.LOCALHOST), localResultFile, resultFolder);
                final UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(activeProject, runTabName);
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    LastTestStorage.setLastTestStorageLocalJmx(runTabName, jmxParameters);
                    TestRunner.runTest(activeProject, Locations.LOCALHOST, jmxParameters, runStrategy, uiDescriptor);
                });
            }
        };
    }

}