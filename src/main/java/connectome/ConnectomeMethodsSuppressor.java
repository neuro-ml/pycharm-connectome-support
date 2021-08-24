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
import java.util.Set;

public class ConnectomeMethodsSuppressor implements InspectionSuppressor {
    final private Set<String> classes = Set.of("Source", "Transform", "Mixin", "CallableLayer", "BaseLayer");

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
        // suppress errors with fake "self"
        if (element instanceof PyReferenceExpression && toolId.equals("PyTypeChecker")) {
            PsiReference reference = element.getReference();
            if (reference != null && isConnectomeMethodSelf(reference.resolve())) return true;
        }
        if (element instanceof PyTargetExpression && toolId.equals("PyMethodFirstArgAssignment")) {
            PyClass reference = ((PyTargetExpression) element).getContainingClass();
            if (reference != null && isConnectomeClass(reference)) return true;
        }

        if (element instanceof PyReferenceExpression && toolId.equals("PyProtectedMember")) {
            PsiElement[] children = element.getChildren();
            System.out.println(children[0]);
            System.out.println(children[0].getClass());
            if (children.length == 1) {
                PsiElement child = children[0];
                // SomeClass()._field
                if (child instanceof PyCallExpression) {
                    return isConnectomeClassCall((PyCallExpression) child);
                }

                // x = SomeClass()
                // x._field
                if (child instanceof PyReferenceExpression) {
                    PsiElement resolved = ((PyReferenceExpression) child).getReference().resolve();

                    if (resolved instanceof PyTargetExpression) {
                        PyExpression value = ((PyTargetExpression) resolved).findAssignedValue();

                        if (value instanceof PyCallExpression) {
                            return isConnectomeClassCall((PyCallExpression) value);
                        }
                    }
                }
            }
        }

//        PyTypeProvider
//        PyKnownDecoratorProvider
//        PyAnnotationElementType
//        System.out.println(toolId);
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
        return isConnectomeClass(containingClass);
    }

    private boolean isConnectomeClassCall(@NotNull PyCallExpression call) {
        PyExpression callee = call.getCallee();

        if (callee instanceof PyReferenceExpression) {
            PsiElement cls = ((PyReferenceExpression) callee).getReference().resolve();

            if (cls instanceof PyClass) {
                return isConnectomeClass((PyClass) cls);
            }
        }

        return false;
    }

    private boolean isConnectomeClass(@NotNull PyClass cls) {
        // either this is a connectome class
        String className = cls.getName();
        if (classes.contains(className)) {
            QualifiedName canonicalImportPath = QualifiedNameFinder.findCanonicalImportPath(cls, null);
            if (canonicalImportPath != null) {
                for (String component : canonicalImportPath.getComponents()) {
                    if (Objects.equals(component, "connectome")) return true;
                }
            }
//                        TODO: metaclasses?
//                        System.out.println(((PyClass) target).getMetaClassExpression());
//                        VirtualFile virtualFile = target.getContainingFile().getVirtualFile();
//                        Module moduleForPsiElement = ModuleUtil.findModuleForPsiElement(target);
        }

        // or is a subclass
        PyArgumentList superClassExpressionList = cls.getSuperClassExpressionList();
        if (superClassExpressionList == null) return false;

        for (PyExpression argument : superClassExpressionList.getArguments()) {
//            TODO: support other than PyReferenceExpression
            if (argument instanceof PyReferenceExpression) {
                PsiReference reference = argument.getReference();
                if (reference != null) {
                    PsiElement target = reference.resolve();
                    if (target instanceof PyClass && isConnectomeClass((PyClass) target)) return true;
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
