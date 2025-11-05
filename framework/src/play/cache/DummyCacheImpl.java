package play.cache;

import static play.libs.Lazy.lazyEvaluated;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import play.libs.Lazy;

/**
 * This class implements no caching at all. Since RePlay expects an implementation of the CacheImpl
 * interface to be present, we have created this "dummy" implementation that does nothing.
 * All `get` calls "miss", and `set` calls do nothing on purpose.
 */
@NullMarked
@CheckReturnValue
public class DummyCacheImpl implements CacheImpl {

  private static final Lazy<DummyCacheImpl> uniqueInstance = lazyEvaluated(() -> new DummyCacheImpl());

  private DummyCacheImpl() {}

  public static DummyCacheImpl instance() {
    return uniqueInstance.get();
  }

  @Override
  public void set(String key, @Nullable Object value, int expiration) {}

  @Nullable
  @Override
  public Object get(String key) {
    return null;
  }

  @Override
  public void clear() {}

  @Override
  public void delete(String key) {}

  @Override
  public void stop() {}
}
