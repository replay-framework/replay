package play.classloading;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class JavaClassesScanner {
  public List<Class<?>> allClassesInProject() {
    List<Class<?>> result = new ArrayList<>();

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

  private List<Class<?>> classesInDirectory(String packageName, File directory) throws ClassNotFoundException {
    if (directory.getAbsolutePath().contains("/test"))
      return emptyList();

    List<Class<?>> result = new ArrayList<>();
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        String subPackage = packageName == null ? file.getName() : packageName + '.' + file.getName();
        result.addAll(classesInDirectory(subPackage, file));
      }
      else if (file.getName().endsWith(".class")) {
        String className = packageName == null ? classNameOf(file) : packageName + '.' + classNameOf(file);
        result.add(Class.forName(className));
      }
    }
    return result;
  }

  private String classNameOf(File file) {
    return file.getName().replace(".class", "");
  }
}
