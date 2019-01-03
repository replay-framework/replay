package play.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A cache implementation.
 * expiration is specified in seconds
 * @see play.cache.Cache
 */
public interface CacheImpl {
    void set(@Nonnull String key, Object value, int expiration);

    @Nullable Object get(@Nonnull String key);

    void clear();

    void delete(@Nonnull String key);

    void stop();
}
