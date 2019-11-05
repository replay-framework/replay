package play.cache;

import net.spy.memcached.MemcachedClient;
import org.slf4j.MDC;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Memcached implementation (using http://code.google.com/p/spymemcached/)
 * <p>
 * expiration is specified in seconds
 */
public class MemcachedImpl implements CacheImpl {
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
    Future<Object> future = client.asyncGet(key, new MDCAwareTranscoder(tc, mdcParameterName, MDC.get(mdcParameterName)));
    try {
      return future.get(1, TimeUnit.SECONDS);
    }
    catch (Exception e) {
      future.cancel(false);
    }
    return null;
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
