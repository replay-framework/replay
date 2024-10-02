package play.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Properties;

/**
 * This class implements no caching at all. Since RePlay expects an implementation of the CacheImpl
 * interface to be present, we have created this "dummy" implementation that does nothing.
 * All `get` calls "miss", and `set` calls do nothing on purpose.
 */
public class DummyCacheImpl implements CacheImpl {

  private static DummyCacheImpl uniqueInstance = null;

  private DummyCacheImpl() {}

  public static DummyCacheImpl instance(@SuppressWarnings("unused") Properties playProperties) {
    if (uniqueInstance == null) {
      uniqueInstance = new DummyCacheImpl();
    }
    return uniqueInstance;
  }

  @Override
  public void set(@Nonnull String key, Object value, int expiration) {}

  @Nullable
  @Override
  public Object get(@Nonnull String key) {
    return null;
  }

  @Override
  public void clear() {}

  @Override
  public void delete(@Nonnull String key) {}

  @Override
  public void stop() {}
}
