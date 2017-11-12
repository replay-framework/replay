package play.rebel;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.ApplicationClassloader;
import play.classloading.RebelClassloader;

import java.util.List;

public class PlayRebelAntiEnhancerPlugin extends PlayPlugin {
  RebelClassloader rebelClassloader = new RebelClassloader();

  public PlayRebelAntiEnhancerPlugin() {
    Logger.info("REBEL: Play enhancers disabled");

    if (enabled()) {
      Logger.info("REBEL: Play compilation also disabled");
      Play.classloader = rebelClassloader;
    }
    else {
      Logger.info("REBEL: Play compilation enabled");
    }
  }

  @Override public void onConfigurationRead() {
    if (enabled()) {
      Logger.info("REBEL: Play compilation disabled");
      Play.classloader = rebelClassloader;
      resetContextClassloader(rebelClassloader);
    }
    else {
      Logger.info("REBEL: Play compilation enabled");
    }
  }

  private boolean enabled() {
    return Play.mode.isDev() && !Play.usePrecompiled && !Play.forceProd && System.getProperty("precompile") == null;
  }

  private void resetContextClassloader(ApplicationClassloader classloader) {
    Thread thread = Thread.currentThread();
    if (thread.getContextClassLoader() instanceof ApplicationClassloader)
      thread.setContextClassLoader(classloader);
  }

  @Override public final boolean compileSources() {
    if (enabled()) {
      initPlayClasses();
      return true;
    }
    else {
      return false;
    }
  }

  private void initPlayClasses() {
    List<Class<?>> allClasses = JavaClasses.allClassesInProject();

    for (Class<?> javaClass : allClasses) {
      ApplicationClass appClass = new ApplicationClass(javaClass.getName());
      appClass.javaClass = javaClass;
      Play.classes.add(appClass);
    }
  }
}
