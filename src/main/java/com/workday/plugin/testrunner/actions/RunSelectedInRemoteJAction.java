package com.workday.plugin.testrunner.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.sm.runner.SMTRunnerNodeDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
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

    private static final String KEY_LAST_CATEGORY = "oms.lastCategory";

    private final JComponent viewComp;
    private final Project project;

    public RunSelectedInRemoteJAction(JComponent viewComp, Project project) {
        super("Run selected", "Run selected test", AllIcons.Actions.Execute);
        this.viewComp = viewComp;
        this.project = project;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        execute(getSelectedProxy(), project);
    }

    public static void execute(AbstractTestProxy proxy, Project project) {
        if (proxy == null || isVariantLeaf(proxy)) return;

        TestLocation loc = parseLocation(proxy, project);
        if (loc == null) return;

        LastTestStorage.LastTestEntry lastEntry = LastTestStorage.getLastTestEntry();

        // For package nodes via JMX: prompt for category before opening a new tab.
        // RemoteJ ignores category (Gradle --tests has no category filter).
        String[] packageJmxParams = null;
        if (loc.isPackage() && lastEntry != null && (lastEntry.isLocalJmx() || lastEntry.isOrs())) {
            String category = promptForCategory(proxy, project);
            if (category == null) return; // cancelled
            packageJmxParams = ParamBuilder.getPackageArgs(loc.packageName(), category);
        }

        UiContentDescriptor uiDescriptor = UiContentDescriptor.createUiDescriptor(project, loc.tabName);
        UiContentDescriptor.UiProcessHandler handler = uiDescriptor.getUiProcessHandler();

        if (lastEntry == null || lastEntry.isRemoteJ()) {
            RemoteJRunStrategy strategy = new RemoteJRunStrategy();
            strategy.setProcessHandler(handler);
            strategy.runGradleTest(loc.gradleArg);

        } else if (lastEntry.isLocalJmx()) {
            Locations.setBasePath(lastEntry.getBasePath());
            final String[] jmxParams = packageJmxParams != null ? packageJmxParams : loc.toJmxArgs();
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                LocalRunStrategy runStrategy = new LocalRunStrategy(
                        new OSCommands(Locations.LOCALHOST), Locations.getLocalResultFile(), Locations.getBasePath());
                runStrategy.setProcessHandler(handler);
                runStrategy.runJmxTest(jmxParams);
            });

        } else if (lastEntry.isOrs()) {
            String host = lastEntry.getHost();
            if (host == null || host.isBlank()) {
                ReRunLastTestAction.showBalloon(project, "Host is not specified");
                return;
            }
            Locations.setBasePath(lastEntry.getBasePath());
            final String[] jmxParams = packageJmxParams != null ? packageJmxParams : loc.toJmxArgs();
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

    /**
     * Prompts for a test category. Pre-fills with the category guessed from the single class
     * in the package (via its @Tag annotation), falling back to the last saved value.
     * Returns null if the dialog is cancelled.
     */
    @Nullable
    private static String promptForCategory(AbstractTestProxy packageProxy, Project project) {
        String guessed = guessCategory(packageProxy, project);
        String last = PropertiesComponent.getInstance().getValue(KEY_LAST_CATEGORY, "");
        String initial = guessed != null ? guessed : last;

        String input = Messages.showInputDialog(
                project,
                "Test category (e.g. OMSBI, OMSBASE):",
                "Run Package",
                null,
                initial,
                null);
        if (input == null) return null;
        input = input.trim();
        if (!input.isEmpty()) {
            PropertiesComponent.getInstance().setValue(KEY_LAST_CATEGORY, input);
        }
        return input.isEmpty() ? "empty" : input;
    }

    /**
     * If the package subtree contains exactly one test class, reads its @Tag annotation
     * value via PSI constant evaluation. Returns null if the category cannot be determined.
     */
    @Nullable
    private static String guessCategory(AbstractTestProxy packageProxy, Project project) {
        List<String> classNames = new ArrayList<>();
        collectClassNames(packageProxy, classNames);
        if (classNames.size() != 1) return null;

        com.intellij.psi.PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(classNames.get(0), GlobalSearchScope.allScope(project));
        if (psiClass == null) return null;

        com.intellij.psi.PsiAnnotation tag = psiClass.getAnnotation("org.junit.jupiter.api.Tag");
        if (tag == null) return null;

        com.intellij.psi.PsiAnnotationMemberValue value = tag.findAttributeValue("value");
        if (value == null) return null;

        Object constant = JavaPsiFacade.getInstance(project)
                .getConstantEvaluationHelper()
                .computeConstantExpression(value);
        return constant instanceof String ? (String) constant : null;
    }

    /** Recursively collects class FQNs (java: nodes without #) from a proxy subtree. */
    private static void collectClassNames(AbstractTestProxy proxy, List<String> result) {
        for (AbstractTestProxy child : proxy.getChildren()) {
            String url = child.getLocationUrl();
            if (url == null) continue;
            if (url.startsWith("java:") && !url.contains("#")) {
                result.add(url.substring("java:".length()));
            } else if (url.startsWith("pkg:")) {
                collectClassNames(child, result);
            }
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        AbstractTestProxy proxy = getSelectedProxy();
        boolean enabled = proxy != null && !isVariantLeaf(proxy);
        e.getPresentation().setEnabled(enabled);
        if (enabled) {
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
        if (url == null || (!url.startsWith("java:") && !url.startsWith("pkg:"))) return null;

        return proxy;
    }

    /**
     * Returns true if proxy is an individual parameterized variant (leaf whose parent is
     * a method-group suite). Such nodes share the same locationUrl as the method group and
     * cannot be targeted individually, so the Run button is disabled for them.
     */
    public static boolean isVariantLeaf(AbstractTestProxy proxy) {
        if (!proxy.getChildren().isEmpty()) return false;
        AbstractTestProxy parent = proxy.getParent();
        if (parent == null) return false;
        String parentUrl = parent.getLocationUrl();
        return parentUrl != null && parentUrl.contains("#");
    }

    @Nullable
    private static TestLocation parseLocation(AbstractTestProxy proxy, Project project) {
        String url = proxy.getLocationUrl();

        if (url.startsWith("pkg:")) {
            String packageName = url.substring("pkg:".length());
            String shortPkg = packageName.substring(packageName.lastIndexOf('.') + 1);
            return new TestLocation(null, null, null,
                    packageName + ".*", shortPkg + "@run", packageName);
        }

        String javaPath = url.substring("java:".length());
        String[] parts = javaPath.split("#", 2);
        String className = parts[0];
        if (className.isBlank()) return null;

        String shortClass = className.substring(className.lastIndexOf('.') + 1);

        if (parts.length < 2 || parts[1].isBlank()) {
            return new TestLocation(className, null, null, className, shortClass + "@run", null);
        }

        String methodName = parts[1];
        String paramSuffix = buildParamTypesSuffix(className, methodName, project);
        String jmxMethodSig = className + "@" + methodName + paramSuffix;
        return new TestLocation(className, methodName, jmxMethodSig,
                className + "." + methodName, methodName + "@run", null);
    }

    /** Looks up parameter types via PSI so ORS can resolve parameterized test methods. */
    private static String buildParamTypesSuffix(String className, String methodName, Project project) {
        com.intellij.psi.PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(className, GlobalSearchScope.allScope(project));
        if (psiClass == null) return "";
        PsiMethod[] methods = psiClass.findMethodsByName(methodName, false);
        if (methods.length == 0) return "";
        com.intellij.psi.PsiParameter[] params = methods[0].getParameterList().getParameters();
        if (params.length == 0) return "";
        String paramTypes = Arrays.stream(params)
                .map(p -> p.getType().getCanonicalText())
                .collect(Collectors.joining(","));
        return "(" + paramTypes + ")";
    }

    private record TestLocation(@Nullable String className, @Nullable String methodName,
                                @Nullable String jmxMethodSig, String gradleArg, String tabName,
                                @Nullable String packageName) {
        boolean isPackage() { return packageName != null; }

        String[] toJmxArgs() {
            if (isPackage()) return ParamBuilder.getPackageArgs(packageName, "empty");
            if (methodName != null) return ParamBuilder.getMethodArgs(jmxMethodSig);
            return ParamBuilder.getClassArgs(className);
        }
    }
}
