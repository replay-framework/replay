package play.classloading;

import play.Play;
import play.template2.compile.GTTypeResolver;


public class GTTypeResolver1xImpl implements GTTypeResolver {
  @Override public boolean isApplicationClass(String className) {
    return Play.classes.getApplicationClass(className) != null;
  }
}
