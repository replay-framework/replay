package play.cache;

import net.sf.oval.exception.InvalidConfigurationException;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.ExpiryPolicy;
import play.Play;

import java.io.Serializable;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toMap;
import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;
import static org.ehcache.config.units.EntryUnit.ENTRIES;
import static org.ehcache.config.units.MemoryUnit.MB;

/**
 * EhCache implementation.
 *
 * <p>Ehcache is an open source, standards-based cache used to boost performance,
 * offload the database and simplify scalability. Ehcache is robust, proven and
 * full-featured and this has made it the most widely-used Java-based cache.</p>
 *
 * Expiration is specified in seconds
 * 
 * @see <a href="http://ehcache.org/">http://ehcache.org/</a>
 *
 */
public class EhCacheImpl implements CacheImpl {

    private static EhCacheImpl uniqueInstance;

    CacheManager cacheManager;

    Cache<String, ValueWrapper> cache;

    private static final String cacheName = "play";

    private EhCacheImpl() {
        long heapSizeInMb = Long.valueOf(Play.configuration.getProperty("ehcache.heapSizeInMb", "0"));
        long heapSizeInEntries = Long.valueOf(Play.configuration.getProperty("ehcache.heapSizeInEntries", "0"));
        long offHeapSizeInMb = Long.valueOf(Play.configuration.getProperty("ehcache.offHeapSizeInMb", "0"));
        if (heapSizeInMb == 0 && heapSizeInEntries == 0 && offHeapSizeInMb == 0)
            throw new InvalidConfigurationException("Must specify nonzero ehcache.heapSizeInMb/ehcache.heapSizeInEntries or ehcache.offHeapSizeInMb");

        ResourcePoolsBuilder heapBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder();
        if (heapSizeInMb > 0) heapBuilder = heapBuilder.heap(heapSizeInMb, MB);
        if (heapSizeInEntries > 0) heapBuilder = heapBuilder.heap(heapSizeInEntries, ENTRIES);
        if (offHeapSizeInMb > 0) heapBuilder = heapBuilder.offheap(offHeapSizeInMb, MB);

        CacheConfigurationBuilder<String, ValueWrapper> configurationBuilder =
                newCacheConfigurationBuilder(String.class, ValueWrapper.class, heapBuilder)
                .withExpiry(new ValueWrapperAwareExpiry());
        this.cacheManager = newCacheManagerBuilder().withCache(cacheName, configurationBuilder).build(true);
        this.cache = cacheManager.getCache(cacheName, String.class, ValueWrapper.class);
    }

    public static EhCacheImpl getInstance() {
        return uniqueInstance;
    }

    public static EhCacheImpl newInstance() {
        uniqueInstance = new EhCacheImpl();
        return uniqueInstance;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void delete(String key) {
        cache.remove(key);
    }

    @Override
    public Object get(String key) {
        ValueWrapper valueWrapper = cache.get(key);
        return valueWrapper == null ? null : valueWrapper.value;
    }

    @Override
    public Map<String, Object> get(String[] keys) {
        return cache.getAll(new HashSet<>(asList(keys))).entrySet()
                .stream().collect(toMap(Map.Entry::getKey, e -> e.getValue().value));
    }

    @Override
    public void set(String key, Object value, int expiration) {
        cache.put(key, new ValueWrapper(value, expiration));
    }

    @Override
    public void stop() {
        cacheManager.close();
    }

    private static class ValueWrapper implements Serializable {
        Object value;
        int expiration;

        ValueWrapper(Object value, int expiration) {
            this.value = value;
            this.expiration = expiration;
        }
    }

    private static class ValueWrapperAwareExpiry implements ExpiryPolicy<String, ValueWrapper> {
        @Override
        public Duration getExpiryForCreation(String key, ValueWrapper value) {
            return Duration.ofSeconds(value.expiration);
        }

        @Override
        public Duration getExpiryForAccess(String key, Supplier<? extends ValueWrapper> value) {
            return null;
        }

        @Override
        public Duration getExpiryForUpdate(String key, Supplier<? extends ValueWrapper> oldValue, ValueWrapper newValue) {
            return Duration.ofSeconds(newValue.expiration);
        }
    }
}
