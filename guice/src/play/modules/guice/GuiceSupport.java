package play.modules.guice;

import com.google.inject.Injector;

/**
 * Implemented if a custom injector is desired
 */
public abstract class GuiceSupport {
  protected abstract Injector configure();
}
