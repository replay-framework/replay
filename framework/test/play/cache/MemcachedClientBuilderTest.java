package play.cache;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Properties;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class MemcachedClientBuilderTest {
  @Test
  public void singleAddress() {
    Properties configuration = new Properties();
    configuration.setProperty("memcached.host", "127.3.4.5:22344");

    assertThat(new MemcachedClientBuilder().parseAddresses(configuration)).isEqualTo(
      asList(new InetSocketAddress("127.3.4.5", 22344))
    );
  }

  @Test
  public void multipleAddress() {
    Properties configuration = new Properties();
    configuration.setProperty("memcached.1.host", "127.3.4.5:22344");
    configuration.setProperty("memcached.2.host", "128.6.7.888:55555");
    configuration.setProperty("memcached.4.host", "128.6.7.888:66666");

    assertThat(new MemcachedClientBuilder().parseAddresses(configuration)).isEqualTo(asList(
      new InetSocketAddress("127.3.4.5", 22344),
      new InetSocketAddress("128.6.7.888", 55555))
    );
  }
}