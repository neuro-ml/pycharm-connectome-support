package connectome;

import com.jetbrains.python.psi.PyKnownDecoratorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class FieldDecorators implements PyKnownDecoratorProvider {
    final private Set<String> decorators = Set.of("meta", "positional", "inverse", "optional", "insert");

    @Nullable
    @Override
    public String toKnownDecorator(@NotNull String decoratorName) {
        return decorators.contains(decoratorName) ? "staticmethod" : null;
    }
}
