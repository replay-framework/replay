package play.classloading;

import play.Play;
import play.template2.compile.GTTypeResolver;


public class GTTypeResolver1xImpl implements GTTypeResolver {
  @Override public byte[] getTypeBytes(String name) {
    ApplicationClasses.ApplicationClass applicationClass = Play.classes.getApplicationClass(name);

    if (applicationClass != null && applicationClass.javaByteCode != null) {
      return applicationClass.javaByteCode;
    }

    return Play.classloader.getClassDefinition(name);
  }

  @Override public boolean isApplicationClass(String className) {
    return Play.classes.getApplicationClass(className) != null;
  }
}
