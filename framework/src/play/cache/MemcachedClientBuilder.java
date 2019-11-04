package play.cache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.auth.PlainCallbackHandler;
import play.exceptions.ConfigurationException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;

class MemcachedClientBuilder {
  public MemcachedClient build(Properties configuration) throws IOException {
    List<InetSocketAddress> addrs;
    if (configuration.containsKey("memcached.host")) {
      addrs = AddrUtil.getAddresses(configuration.getProperty("memcached.host"));
    } else if (configuration.containsKey("memcached.1.host")) {
      int nb = 1;
      String addresses = "";
      while (configuration.containsKey("memcached." + nb + ".host")) {
        addresses += configuration.get("memcached." + nb + ".host") + " ";
        nb++;
      }
      addrs = AddrUtil.getAddresses(addresses);
    } else {
      throw new ConfigurationException("Bad configuration for memcached: missing host(s)");
    }

    if (configuration.containsKey("memcached.user")) {
      String memcacheUser = configuration.getProperty("memcached.user");
      String memcachePassword = configuration.getProperty("memcached.password");
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

      return new MemcachedClient(cf, addrs);
    } else {
      return new MemcachedClient(addrs);
    }
  }
}
