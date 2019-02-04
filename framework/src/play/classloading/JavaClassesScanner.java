package play.classloading;

import io.github.classgraph.ClassGraph;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class JavaClassesScanner {
  public List<Class<?>> allClassesInProject() {
    List<Class<?>> result = new ArrayList<>();

    List<File> classpath = new ClassGraph().getClasspathFiles();

    for (File file : classpath) {
      try {
        if (file.isDirectory()) result.addAll(classesInDirectory(null, file));
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    return result;
  }

  private List<Class<?>> classesInDirectory(String packageName, File directory) throws ClassNotFoundException {
    if (directory.getAbsolutePath().contains("/test"))
      return emptyList();
    if (directory.getAbsolutePath().contains("pdf/build/thirdParty"))
      return emptyList(); // it causes initialisation of org.xhtmlrenderer.swing.AWTFSImage which is slow

    List<Class<?>> result = new ArrayList<>();
    for (File file : directory.listFiles()) {
      if (file.isDirectory()) {
        String subPackage = packageName == null ? file.getName() : packageName + '.' + file.getName();
        result.addAll(classesInDirectory(subPackage, file));
      }
      else if (file.getName().endsWith(".class")) {
        String className = packageName == null ? classNameOf(file) : packageName + '.' + classNameOf(file);
        result.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
      }
    }
    return result;
  }

  private String classNameOf(File file) {
    return file.getName().replace(".class", "");
  }
}
