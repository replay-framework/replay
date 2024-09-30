package play.cache;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.CacheException;
import play.exceptions.ConfigurationException;
import play.libs.Time;

/**
 * The Cache. Mainly an interface to Memcached or EhCache.
 *
 * Choose cache implementation by having your project depend on either one of these RePlay
 * packages: `memcached` or `ehcache`. Without one of these packages the fall-back "dummy" cache
 * is used, which does not do any caching.
 *
 * <p>Note: When specifying expiration == "0s" (zero seconds) the actual expiration-time may vary
 * between different cache implementations
 */
public abstract class Cache {

  private static final Logger logger = LoggerFactory.getLogger(Cache.class);

  /** The underlying cache implementation */
  public static CacheImpl cacheImpl;

  /**
   * Set an element.
   *
   * @param key        Element key
   * @param value      Element value
   * @param expiration Ex: 10s, 3mn, 8h
   */
  public static void set(String key, Object value, String expiration) {
    checkSerializable(value);
    cacheImpl.set(key, value, Time.parseDuration(expiration));
  }

  /**
   * Retrieve an object.
   *
   * @param key The element key
   * @return The element value or null
   */
  @Nullable
  public static <T> T get(@Nonnull String key) {
    return (T) cacheImpl.get(key);
  }

  /**
   * Delete an element from the cache.
   *
   * @param key The element key
   */
  public static void delete(String key) {
    cacheImpl.delete(key);
  }

  /** Clear all data from cache. */
  public static void clear() {
    if (cacheImpl != null) {
      cacheImpl.clear();
    }
  }

  /** Initialize the cache system. */
  public static void init() {
    Class<?> memcacheImplClass = null;
    try {
      memcacheImplClass = Class.forName("play.cache.MemcachedImpl");
    } catch (ClassNotFoundException e) {
      // Do nothing, leaving cacheImplClass null on purpose.
    }

    Class<?> ehCacheImplClass = null;
    try {
      ehCacheImplClass = Class.forName("play.cache.EhCacheImpl");
    } catch (ClassNotFoundException e) {
      // Do nothing, leaving cacheImplClass null on purpose.
    }

    if (memcacheImplClass != null && ehCacheImplClass != null) {
      throw new ConfigurationException(
          "Please remove either RePlay's `memcached` or `ehcache` package from the classpath");
    }

    Class<?> cacheImplClass = memcacheImplClass == null ? ehCacheImplClass : memcacheImplClass;
    if (cacheImplClass != null) {
      try {
        // Since it implements the `CacheImpl` interface, it should have a static `instance` method.
        Method method = cacheImplClass.getDeclaredMethod("instance", Properties.class);
        cacheImpl = (CacheImpl) method.invoke(null, Play.configuration);
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
        logger.error("Error while instantiating cache", e);
        throw new ConfigurationException("Failed to start the caching system");
      }
    } else {
      cacheImpl = DummyCacheImpl.instance(new Properties());
    }
  }

  /** Stop the cache system. */
  public static void stop() {
    try {
      cacheImpl.stop();
    } catch (Exception e) {
      logger.error("Failed to stop the cache", e);
    }
  }

  /** Utility that check that an object is serializable. */
  static void checkSerializable(Object value) {
    if (value != null && !(value instanceof Serializable)) {
      throw new CacheException(
          "Cannot cache a non-serializable value of type " + value.getClass().getName(),
          new NotSerializableException(value.getClass().getName()));
    }
  }
}
