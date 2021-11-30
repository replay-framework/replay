package play.cache;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Memcached implementation (using http://code.google.com/p/spymemcached/)
 * <p>
 * expiration is specified in seconds
 */
public class MemcachedImpl implements CacheImpl {
  private static final Logger logger = LoggerFactory.getLogger(MemcachedImpl.class);
  private final MemcachedClient client;
  private final String mdcParameterName;
  private final MemcachedTranscoder tc = new MemcachedTranscoder();

  public MemcachedImpl(Properties configuration) throws IOException {
    System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");
    client = new MemcachedClientBuilder().build(configuration);
    mdcParameterName = configuration.getProperty("memcached.mdc.parameter", "");
  }

  public MemcachedClient getClient() {
    return client;
  }

  @Override
  @Nullable
  public Object get(@Nonnull String key) {
    Future<Object> future = client.asyncGet(key, transcoder());
    try {
      return future.get(1, TimeUnit.SECONDS);
    }
    catch (TimeoutException | InterruptedException e) {
      logger.warn("Cache miss due to timeout. key={}, cause={}, connection={}, connectionStatus={}",
        key, e, getConnectionDescription(), getConnectionStatus());
      future.cancel(true);
    }
    catch (ExecutionException e) {
      logger.error("Cache miss due to error. key={}, connection={}, connectionStatus={}",
        key, getConnectionDescription(), getConnectionStatus(), e);
      future.cancel(true);
    }
    return null;
  }

  private String getConnectionDescription() {
    try {
      return client.getConnection().connectionsStatus();
    }
    catch (RuntimeException e) {
      return "Failed to get connection details: " + e;
    }
  }

  private String getConnectionStatus() {
    try {
      return client.getConnection().connectionsStatus();
    }
    catch (RuntimeException e) {
      return "Failed to check connection status: " + e;
    }
  }

  @Nonnull SerializingTranscoder transcoder() {
    return isEmpty(mdcParameterName) ?
      tc :
      new MDCAwareTranscoder(tc, mdcParameterName, MDC.get(mdcParameterName));
  }

  @Override
  public void clear() {
    client.flush();
  }

  @Override
  public void delete(@Nonnull String key) {
    client.delete(key);
  }

  @Override
  public void set(@Nonnull String key, Object value, int expiration) {
    client.set(key, expiration, value, tc);
  }

  @Override
  public void stop() {
    client.shutdown();
  }
}
