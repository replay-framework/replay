package play;

import static java.lang.System.currentTimeMillis;

import java.io.IOException;
import play.exceptions.UnexpectedException;
import play.i18n.Lang;
import play.mvc.Http;

/** An Invocation in something to run in a Play! context */
public abstract class Invocation implements Runnable {

  /** Override this method */
  public abstract void execute();

  /**
   * Needs this method to do stuff *before* init() is executed. The different
   * Invocation-implementations does a lot of stuff in init() and they might do it before calling
   * super.init()
   */
  protected void preInit() {
    // Clear the locale (Lang) for this request. We're resolving it later when it is needed.
    Lang.clear();
  }

  /**
   * Init the call (especially useful in DEV mode to detect changes)
   *
   * @return true if successful
   */
  public boolean init() throws IOException {
    if (Play.mode.isDev()) {
      for (long start = currentTimeMillis();
          !Play.started && currentTimeMillis() - start < 60000;
          ) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          break;
        }
      }
    }
    if (!Play.started) {
      throw new IllegalStateException("Application is not started");
    }
    if (Play.mode.isDev()) {
      Play.detectChanges();
    }
    InvocationContext.current.set(getInvocationContext());
    return true;
  }

  public abstract InvocationContext getInvocationContext();

  /** Things to do before an Invocation */
  public void before() {
    Play.pluginCollection.beforeInvocation();
  }

  /** Things to do after an Invocation. (if the Invocation code has not thrown any exception) */
  public void after() {
    Play.pluginCollection.afterInvocation();
  }

  /** Things to do when the whole invocation has succeeded (before + execute + after) */
  public void onSuccess() throws Exception {
    Play.pluginCollection.onInvocationSuccess();
  }

  public void onActionInvocationException(
      Http.Request request, Http.Response response, Throwable e) {
    Play.pluginCollection.onActionInvocationException(request, response, e);
    if (e instanceof RuntimeException) {
      throw (RuntimeException) e;
    }
    throw new UnexpectedException(e);
  }
}
