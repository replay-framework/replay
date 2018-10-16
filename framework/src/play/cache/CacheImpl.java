package play.cache;

import java.util.Map;

/**
 * A cache implementation.
 * expiration is specified in seconds
 * @see play.cache.Cache
 */
public interface CacheImpl {
    public void set(String key, Object value, int expiration);

    public Object get(String key);

    public Map<String, Object> get(String[] keys);

    public void clear();

    public void delete(String key);

    public void stop();
}
