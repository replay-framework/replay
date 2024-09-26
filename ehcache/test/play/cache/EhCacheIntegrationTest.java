package play.cache;

import org.junit.jupiter.api.Test;
import play.Play;

import static org.assertj.core.api.Assertions.assertThat;

public class EhCacheIntegrationTest {

  @Test
  public void initializationWithinRePlay() {
    Play.configuration.setProperty("ehcache.heapSizeInEntries", "100");
    Cache.init();
    Cache.set("no-dummy", 1, "10s");
    assertThat((int) Cache.get("no-dummy")).isEqualTo(1);
    assertThat(Cache.cacheImpl).isInstanceOf(EhCacheImpl.class);
  }
}
