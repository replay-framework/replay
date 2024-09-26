package play.cache;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DummyCacheIntegrationTest {

  @Test
  public void initializationWithinRePlay() {
    Cache.init();
    Cache.set("is-dummy", 1, "10s");
    assertThat((Object) Cache.get("is-dummy")).isNull(); // null indicates a cache "miss"
    assertThat(Cache.cacheImpl).isInstanceOf(DummyCacheImpl.class);
  }
}
