package play.libs;

import static java.util.Objects.requireNonNull;

import java.util.function.Supplier;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@CheckReturnValue
public class Lazy<T> {
  @Nullable
  private volatile T value;
  private final Object lock = new Object();

  private final Supplier<T> supplier;

  private Lazy(Supplier<T> supplier) {
    this.supplier = supplier;
  }

  @NonNull
  public T get() {
    if (value == null) {
      synchronized (lock) {
        if (value == null) {
          value = supplier.get();
        }
      }
    }
    return requireNonNull(value);
  }

  boolean isInitialized() {
    return value != null;
  }

  public static <T> Lazy<T> lazyEvaluated(Supplier<T> supplier) {
    return new Lazy<>(supplier);
  }
}
