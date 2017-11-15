package play.libs;

/**
 * The same as @{link java.util.function.Supplier}, but with "throws Exception"
 */
@FunctionalInterface
public interface SupplierWithException<T> {

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws Exception;
}
