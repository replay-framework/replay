package play.cache;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A cache implementation. expiration is specified in seconds.
 * <p>
 * When implementing this interface make sure to provide a static method with this signature:
 *
 * <pre>{@code
 * static CacheImpl instance(@NonNull Properties playProperties) throws IOException
 * }</pre>
 *
 * This method is used by RePlay's {@link play.cache.Cache} class to load the implementation.
 *
 * @see play.cache.Cache and RePlay's 'memcached' and 'ehcache' packages.
 */
@NullMarked
@CheckReturnValue
public interface CacheImpl {

  void set(String key, @Nullable Object value, int expiration);

  @Nullable
  Object get(String key);

  void clear();

  void delete(String key);

  void stop();
}
