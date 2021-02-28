package connectome;

import com.intellij.codeInspection.InspectionSuppressor;
import com.intellij.codeInspection.SuppressQuickFix;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.QualifiedName;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.resolve.QualifiedNameFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ConnectomeMethodsSuppressor implements InspectionSuppressor {
    @Override
    public boolean isSuppressedFor(@NotNull PsiElement element, @NotNull String toolId) {
        // only suppress Python-related stuff
        if (!toolId.startsWith("Py")) return false;

        // suppress self name problems
        if (toolId.equals("PyMethodParameters")) {
            return isConnectomeMethodSelf(element);
        }
        // suppress PyMethodMayBeStatic
        if (toolId.equals("PyMethodMayBeStatic")) {
            PsiElement parent = element.getParent();
            if (!(parent instanceof PyFunction)) return false;
            return isConnectomeMethod((PyFunction) parent);
        }
        // suppress type error with fake "self"
        if (element instanceof PyReferenceExpression && toolId.equals("PyTypeChecker")) {
            PsiReference reference = element.getReference();
            if (reference != null) {
                return isConnectomeMethodSelf(reference.resolve());
            }
        }

//        PyTypeProvider
//        PyKnownDecoratorProvider
//        PyAnnotationElementType
        return false;
    }

    private boolean isConnectomeMethodSelf(@Nullable PsiElement element) {
        if (element instanceof PyNamedParameter && ((PyNamedParameter) element).isSelf()) {
            PsiElement parent = element.getParent().getParent();
            if (!(parent instanceof PyFunction)) return false;
            return isConnectomeMethod((PyFunction) parent);
        }
        return false;
    }

    private boolean isConnectomeMethod(@NotNull PyFunction function) {
        PyClass containingClass = function.getContainingClass();
        if (containingClass == null) return false;
        PyArgumentList superClassExpressionList = containingClass.getSuperClassExpressionList();
        if (superClassExpressionList == null) return false;

        for (PyExpression argument : superClassExpressionList.getArguments()) {
//            TODO: support other than PyReferenceExpression
            if (argument instanceof PyReferenceExpression) {
                PsiReference reference = argument.getReference();
                if (reference != null) {
                    PsiElement target = reference.resolve();
                    if (target instanceof PyClass) {
                        String className = ((PyClass) target).getName();
                        if (!(Objects.equals(className, "Source") || Objects.equals(className, "Transform"))) continue;

                        QualifiedName canonicalImportPath = QualifiedNameFinder.findCanonicalImportPath(target, null);
                        if (canonicalImportPath != null) {
                            if (Objects.equals(canonicalImportPath.getFirstComponent(), "connectome")) return true;
                        }

//                        TODO:
//                        System.out.println(((PyClass) target).getMetaClassExpression());
//                        VirtualFile virtualFile = target.getContainingFile().getVirtualFile();
//                        Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(target);
                    }
                }
            }
        }
        return false;
    }

    @NotNull
    @Override
    public SuppressQuickFix[] getSuppressActions(@Nullable PsiElement element, @NotNull String toolId) {
        return new SuppressQuickFix[0];
    }
}
