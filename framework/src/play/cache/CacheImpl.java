package play.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A cache implementation. expiration is specified in seconds.
 * <p>
 * When implementing this interface make sure to provide a static method with this signature:
 *
 * <pre>{@code
 * static CacheImpl instance(@Nonnull Properties playProperties) throws IOException
 * }</pre>
 *
 * This method is used by RePlay's {@link play.cache.Cache} class to load the implementation.
 *
 * @see play.cache.Cache and RePlay's 'memcached' and 'ehcache' packages.
 */
public interface CacheImpl {

  void set(@Nonnull String key, Object value, int expiration);

  @Nullable
  Object get(@Nonnull String key);

  void clear();

  void delete(@Nonnull String key);

  void stop();
}
