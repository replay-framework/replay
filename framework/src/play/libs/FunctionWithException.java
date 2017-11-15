package play.libs;

/**
 * The same as @{link java.util.function.Function}, but with "throws Exception"
 */
@FunctionalInterface
public interface FunctionWithException<T, R> {
    R apply(T t) throws Exception;
}
