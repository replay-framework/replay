package play.cache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.ConfigurationException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Memcached implementation (using http://code.google.com/p/spymemcached/)
 *
 * expiration is specified in seconds
 */
public class MemcachedImpl implements CacheImpl {
    private static final Logger logger = LoggerFactory.getLogger(MemcachedImpl.class);

    private static MemcachedImpl uniqueInstance;

    MemcachedClient client;

    SerializingTranscoder tc;

    public static MemcachedImpl getInstance() throws IOException {
      return getInstance(false);
    }

    public static MemcachedImpl getInstance(boolean forceClientInit) throws IOException {
        if (uniqueInstance == null) {
            uniqueInstance = new MemcachedImpl();
        } else if (forceClientInit) {
            // When you stop the client, it sets the interrupted state of this thread to true. If you try to reinit it with the same thread in this state,
            // Memcached client errors out. So a simple call to interrupted() will reset this flag
            Thread.interrupted();
            uniqueInstance.initClient();
        }
        return uniqueInstance;

    }

    private MemcachedImpl() throws IOException {
        tc = new SerializingTranscoder() {
            @Override
            protected Object deserialize(byte[] data) {
                try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data)) {
                    @Override
                    protected Class<?> resolveClass(ObjectStreamClass desc)
                      throws ClassNotFoundException {
                        return Class.forName(desc.getName(), false, Thread.currentThread().getContextClassLoader());
                    }
                }) {
                    return in.readObject();
                }
                catch (Exception e) {
                    logger.error("Could not deserialize", e);
                }
                return null;
            }

            @Override
            protected byte[] serialize(Object object) {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                        oos.writeObject(object);
                        return bos.toByteArray();
                    }
                }
                catch (IOException e) {
                    logger.error("Could not serialize", e);
                }
                return null;
            }
        };
        initClient();
    }

    public void initClient() throws IOException {
        System.setProperty("net.spy.log.LoggerImpl", "net.spy.memcached.compat.log.Log4JLogger");
        
        List<InetSocketAddress> addrs;
        if (Play.configuration.containsKey("memcached.host")) {
            addrs = AddrUtil.getAddresses(Play.configuration.getProperty("memcached.host"));
        } else if (Play.configuration.containsKey("memcached.1.host")) {
            int nb = 1;
            String addresses = "";
            while (Play.configuration.containsKey("memcached." + nb + ".host")) {
                addresses += Play.configuration.get("memcached." + nb + ".host") + " ";
                nb++;
            }
            addrs = AddrUtil.getAddresses(addresses);
        } else {
            throw new ConfigurationException("Bad configuration for memcached: missing host(s)");
        }
        
        if (Play.configuration.containsKey("memcached.user")) {
            String memcacheUser = Play.configuration.getProperty("memcached.user");
            String memcachePassword = Play.configuration.getProperty("memcached.password");
            if (memcachePassword == null) {
                throw new ConfigurationException("Bad configuration for memcached: missing password");
            }
            
            // Use plain SASL to connect to memcached
            AuthDescriptor ad = new AuthDescriptor(new String[]{"PLAIN"},
                                    new PlainCallbackHandler(memcacheUser, memcachePassword));
            ConnectionFactory cf = new ConnectionFactoryBuilder()
                                        .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                                        .setAuthDescriptor(ad)
                                        .build();
            
            client = new MemcachedClient(cf, addrs);
        } else {
            client = new MemcachedClient(addrs);
        }
    }

    @Override
    @Nullable
    public Object get(@Nonnull String key) {
        Future<Object> future = client.asyncGet(key, tc);
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
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
