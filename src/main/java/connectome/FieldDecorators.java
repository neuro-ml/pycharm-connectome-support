package connectome;

import com.jetbrains.python.psi.PyKnownDecoratorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FieldDecorators implements PyKnownDecoratorProvider {
    @Nullable
    @Override
    public String toKnownDecorator(@NotNull String decoratorName) {
        return (decoratorName.equals("meta") || decoratorName.equals("field")) ? "staticmethod" : null;
    }
}
