package connectome;

import com.jetbrains.python.psi.PyKnownDecoratorProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FieldDecorators implements PyKnownDecoratorProvider {
    final private Set<String> decorators = Set.of("meta", "positional", "inverse", "optional", "impure");

    @Nullable
    @Override
    public String toKnownDecorator(String decoratorName) {
        return (decoratorName != null && decorators.contains(decoratorName)) ? "staticmethod" : null;
    }
}
