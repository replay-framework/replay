package play.cache;

import net.sf.oval.exception.InvalidConfigurationException;
import org.ehcache.config.ResourcePools;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.Play;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.ehcache.config.ResourceType.Core.HEAP;
import static org.ehcache.config.ResourceType.Core.OFFHEAP;

public class EhCacheImplTest {
    private EhCacheImpl cache;

    @Before
    public void setUp() {
        Play.configuration.setProperty("ehcache.heapSizeInEntries", "100");
        cache = EhCacheImpl.newInstance();
    }

    @After
    public void tearDown() {
        Play.configuration.remove("ehcache.heapSizeInMb");
        Play.configuration.remove("ehcache.offHeapSizeInMb");
    }

    @Test
    public void setAndGet() {
        cache.set("setAndGet", 1, 1);

        assertThat(cache.get("setAndGet")).isEqualTo(1);
    }

    @Test
    public void clear() {
        cache.set("clear", 1, 1);

        cache.clear();

        assertThat(cache.get("clear")).isNull();
    }

    @Test
    public void delete() {
        cache.set("delete1", 1, 1);
        cache.set("delete2", 2, 1);

        cache.delete("delete1");

        assertThat(cache.get("delete1")).isNull();
        assertThat(cache.get("delete2")).isEqualTo(2);
    }

    @Test
    public void stop() {
        try {
            cache.stop();
            cache.set("stop", 1, 1);
            fail("must throw exception");
        }
        catch (IllegalStateException ignored) {}
        finally {
            EhCacheImpl.newInstance();
        }
    }

    @Test
    public void cacheIsConfigurable() {
        Play.configuration.setProperty("ehcache.heapSizeInEntries", "0");
        Play.configuration.setProperty("ehcache.heapSizeInMb", "2");
        Play.configuration.setProperty("ehcache.offHeapSizeInMb", "3");

        EhCacheImpl cache = EhCacheImpl.newInstance();
        ResourcePools resourcePools = cache.cacheManager.getRuntimeConfiguration()
                .getCacheConfigurations().get("play").getResourcePools();

        assertThat(resourcePools.getPoolForResource(HEAP).getSize()).isEqualTo(2);
        assertThat(resourcePools.getPoolForResource(OFFHEAP).getSize()).isEqualTo(3);
    }

    @Test
    public void canSetMaxEntitiesCount() {
        Play.configuration.setProperty("ehcache.heapSizeInEntries", "999");
        Play.configuration.setProperty("ehcache.heapSizeInMb", "0");
        Play.configuration.setProperty("ehcache.offHeapSizeInMb", "0");

        EhCacheImpl cache = EhCacheImpl.newInstance();
        ResourcePools resourcePools = cache.cacheManager.getRuntimeConfiguration()
                .getCacheConfigurations().get("play").getResourcePools();

        assertThat(resourcePools.getPoolForResource(HEAP).getSize()).isEqualTo(999);
    }

    @Test
    public void canNotSetSetMaxSize_bothInMbAndCount() {
        Play.configuration.setProperty("ehcache.heapSizeInEntries", "999");
        Play.configuration.setProperty("ehcache.heapSizeInMb", "10");
        Play.configuration.setProperty("ehcache.offHeapSizeInMb", "0");

        assertThatThrownBy(EhCacheImpl::newInstance)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("configuration already contains");
    }

    @Test
    public void heapStorageWorks() {
        Play.configuration.setProperty("ehcache.heapSizeInEntries", "0");
        Play.configuration.setProperty("ehcache.heapSizeInMb", "1");
        Play.configuration.setProperty("ehcache.offHeapSizeInMb", "0");

        EhCacheImpl cache = EhCacheImpl.newInstance();
        ResourcePools resourcePools = cache.cacheManager.getRuntimeConfiguration()
                .getCacheConfigurations().get("play").getResourcePools();

        assertThat(resourcePools.getPoolForResource(OFFHEAP)).isNull();
        assertThat(resourcePools.getPoolForResource(HEAP).getSize()).isEqualTo(1);

        cache.set("test", 1, 1);
    }

    @Test
    public void offHeapStorageWorks() {
        Play.configuration.setProperty("ehcache.heapSizeInEntries", "0");
        Play.configuration.setProperty("ehcache.heapSizeInMb", "0");
        Play.configuration.setProperty("ehcache.offHeapSizeInMb", "1");

        EhCacheImpl cache = EhCacheImpl.newInstance();
        ResourcePools resourcePools = cache.cacheManager.getRuntimeConfiguration()
                .getCacheConfigurations().get("play").getResourcePools();

        assertThat(resourcePools.getPoolForResource(HEAP)).isNull();
        assertThat(resourcePools.getPoolForResource(OFFHEAP).getSize()).isEqualTo(1);

        cache.set("test", 1, 1);
    }

    @Test(expected = InvalidConfigurationException.class)
    public void mustSpecifyAtLeastOneStorage() {
        Play.configuration.setProperty("ehcache.heapSizeInEntries", "0");
        Play.configuration.setProperty("ehcache.heapSizeInMb", "0");
        Play.configuration.setProperty("ehcache.offHeapSizeInMb", "0");

        EhCacheImpl.newInstance();
    }
}
