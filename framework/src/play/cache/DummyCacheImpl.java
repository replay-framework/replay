package play.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Properties;

public class DummyCacheImpl implements CacheImpl {

  private static DummyCacheImpl uniqueInstance = null;

  @Override
  public CacheImpl instance(@Nonnull Properties playProperties) {
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
