package com.workday.plugin.testrunner.actions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPackage;

import com.workday.plugin.testrunner.target.TestTargetExtractor;

/**
 * Project-view right-click group: "Run OMS". Visible only when a valid OMS test class or
 * package directory is selected. Contains Local JMX and SUV JMX child actions.
 */
public class ProjectViewRunGroup extends DefaultActionGroup {

    static final String KEY_LAST_CATEGORY = "oms.lastCategory";

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(getTarget(e) != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Nullable
    static OmsTarget getTarget(AnActionEvent e) {
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // Directory node → package
        if (element instanceof PsiDirectory dir) {
            PsiPackage pkg = JavaDirectoryService.getInstance().getPackage(dir);
            if (pkg == null || pkg.getQualifiedName().isEmpty()) return null;
            return OmsTarget.forPackage(pkg.getQualifiedName());
        }

        // Java file → look for an OMS test class inside
        if (psiFile instanceof PsiJavaFile javaFile) {
            for (PsiClass clazz : javaFile.getClasses()) {
                if (TestTargetExtractor.isOmsTestClass(clazz)) {
                    return OmsTarget.forClass(clazz.getQualifiedName());
                }
            }
        }

        // PsiClass directly (package/structure view)
        if (element instanceof PsiClass clazz && TestTargetExtractor.isOmsTestClass(clazz)) {
            return OmsTarget.forClass(clazz.getQualifiedName());
        }

        return null;
    }

    @Nullable
    static String promptCategory(Project project) {
        String last = PropertiesComponent.getInstance().getValue(KEY_LAST_CATEGORY, "");
        String input = Messages.showInputDialog(
                project, "Test category (e.g. OMSBI, OMSBASE):", "Run Package", null, last, null);
        if (input == null) return null;
        input = input.trim();
        if (!input.isEmpty()) PropertiesComponent.getInstance().setValue(KEY_LAST_CATEGORY, input);
        return input.isEmpty() ? "empty" : input;
    }

    record OmsTarget(String packageName, String classFqn) {
        static OmsTarget forClass(String fqn) { return new OmsTarget(null, fqn); }
        static OmsTarget forPackage(String pkg) { return new OmsTarget(pkg, null); }
        boolean isPackage() { return packageName != null; }

        String shortName() {
            String s = isPackage() ? packageName : classFqn;
            int dot = s.lastIndexOf('.');
            return dot >= 0 ? s.substring(dot + 1) : s;
        }
    }
}
