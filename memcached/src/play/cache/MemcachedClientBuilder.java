package play.cache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import play.exceptions.ConfigurationException;

class MemcachedClientBuilder {
  public MemcachedClient build(Properties configuration) throws IOException {
    return new MemcachedClient(parseAddresses(configuration));
  }

  List<InetSocketAddress> parseAddresses(Properties configuration) {
    if (configuration.containsKey("memcached.host")) {
      return AddrUtil.getAddresses(configuration.getProperty("memcached.host"));
    } else if (configuration.containsKey("memcached.1.host")) {
      return AddrUtil.getAddresses(buildMultipleAddresses(configuration));
    } else {
      throw new ConfigurationException("Bad configuration for memcached: missing host(s)");
    }
  }

  private String buildMultipleAddresses(Properties configuration) {
    int nb = 1;
    StringBuilder addresses = new StringBuilder();
    while (configuration.containsKey("memcached." + nb + ".host")) {
      addresses.append(configuration.getProperty("memcached." + nb + ".host")).append(" ");
      nb++;
    }
    return addresses.toString();
  }
}
