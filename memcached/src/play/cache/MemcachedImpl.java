package play.cache;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static play.libs.Lazy.lazyEvaluated;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import play.Play;
import play.libs.Lazy;

/**
 * Memcached implementation.
 *
 * <p>Based on: <a href="http://code.google.com/p/spymemcached/">spymemcached</a>).
 *
 * <p>NOTE: expiration is specified in seconds
 */
@SuppressWarnings("unused") // Used through reflection
@NullMarked
@CheckReturnValue
public class MemcachedImpl implements CacheImpl {

  private static final Logger logger = LoggerFactory.getLogger(MemcachedImpl.class);
  private static final Lazy<MemcachedImpl> uniqueInstance = lazyEvaluated(() -> new MemcachedImpl(Play.configuration));
  private final MemcachedClient client;
  private final String mdcParameterName;
  private final MemcachedTranscoder tc = new MemcachedTranscoder();

  private MemcachedImpl(Properties configuration) {
    System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.SLF4JLogger");
    client = new MemcachedClientBuilder().build(configuration);
    mdcParameterName = configuration.getProperty("memcached.mdc.parameter", "");
  }

  @SuppressWarnings("unused") // May be used by implementations
  public MemcachedClient getClient() {
    return client;
  }

  @SuppressWarnings("unused") // Used through reflection
  public static MemcachedImpl instance() {
    return uniqueInstance.get();
  }

  @Override
  @Nullable
  public Object get(String key) {
    Future<Object> future = client.asyncGet(key, transcoder());
    try {
      return future.get(1, TimeUnit.SECONDS);
    } catch (TimeoutException | InterruptedException e) {
      logger.warn(
          "Cache miss due to timeout. key={}, cause={}, connection={}, connectionStatus={}",
          key,
          e,
          getConnectionDescription(),
          getConnectionStatus());
      future.cancel(true);
    } catch (ExecutionException e) {
      logger.error(
          "Cache miss due to error. key={}, connection={}, connectionStatus={}",
          key,
          getConnectionDescription(),
          getConnectionStatus(),
          e);
      future.cancel(true);
    }
    return null;
  }

  private String getConnectionDescription() {
    try {
      return client.getConnection().connectionsStatus();
    } catch (RuntimeException e) {
      return "Failed to get connection details: " + e;
    }
  }

  private String getConnectionStatus() {
    try {
      return client.getConnection().connectionsStatus();
    } catch (RuntimeException e) {
      return "Failed to check connection status: " + e;
    }
  }

  SerializingTranscoder transcoder() {
    return isEmpty(mdcParameterName)
        ? tc
        : new MDCAwareTranscoder(tc, mdcParameterName, MDC.get(mdcParameterName));
  }

  @Override
  public void clear() {
    client.flush();
  }

  @Override
  public void delete(String key) {
    client.delete(key);
  }

  @Override
  public void set(String key, @Nullable Object value, int expiration) {
    client.set(key, expiration, value, tc);
  }

  @Override
  public void stop() {
    client.shutdown();
  }
}
