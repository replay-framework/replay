package play.cache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Properties;
import com.google.errorprone.annotations.CheckReturnValue;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import org.jspecify.annotations.NullMarked;
import play.exceptions.ConfigurationException;

@NullMarked
@CheckReturnValue
class MemcachedClientBuilder {
  public MemcachedClient build(Properties configuration) {
    List<InetSocketAddress> addresses = parseAddresses(configuration);
    try {
      return new MemcachedClient(addresses);
    } catch (IOException e) {
      throw new RuntimeException("Failed to initialize Memcached from " + addresses, e);
    }
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
