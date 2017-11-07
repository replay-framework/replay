package play.rebel;

import com.google.common.base.Joiner;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

public class JavaClasses {
  public static List<Class<?>> allClassesInProject() {
    List<Class<?>> result = newArrayList();

    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (!(classLoader instanceof URLClassLoader)) classLoader = classLoader.getParent();
    URL[] classpath = ((URLClassLoader) classLoader).getURLs();
    for (URL url : classpath) {
      if ("file".equals(url.getProtocol())) {
        try {
          File file = new File(url.toURI());
          if (file.isDirectory()) result.addAll(classesInDirectory(null, file));
        }
        catch (ClassNotFoundException | URISyntaxException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return result;
  }

  private static List<Class<?>> classesInDirectory(String packageName, File directory) throws ClassNotFoundException {
    if (directory.getAbsolutePath().contains("/test"))
      return emptyList();

    List<Class<?>> result = newArrayList();
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        result.addAll(classesInDirectory(Joiner.on(".").skipNulls().join(packageName, file.getName()), file));
      }
      else if (file.getName().endsWith(".class")) {
        result.add(Class.forName(Joiner.on(".").skipNulls().join(packageName, file.getName().replace(".class", ""))));
      }
    }
    return result;
  }
}
