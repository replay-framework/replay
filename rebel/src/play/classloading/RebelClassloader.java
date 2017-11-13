package play.classloading;


import play.Play;

public class RebelClassloader extends ApplicationClassloader {
  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return super.loadClass(name, resolve);
  }

  @Override public Class<?> loadApplicationClass(String name) {
    ApplicationClasses.ApplicationClass applicationClass = Play.classes.classes.get(name);
    return applicationClass == null ? null : applicationClass.javaClass;
  }
}

