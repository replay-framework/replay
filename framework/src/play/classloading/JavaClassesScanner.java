package play.classloading;

import static java.lang.System.nanoTime;
import static java.util.Collections.emptyList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class JavaClassesScanner {
  private static final String SCAN_JARS_PROP = "play.classes.scanJars";

  private static final Logger logger = LoggerFactory.getLogger(JavaClassesScanner.class);
  public List<Class<?>> allClassesInProject() {
    List<Class<?>> result = new ArrayList<>();

    List<File> classpath = new ClassGraph().getClasspathFiles();

    for (File file : classpath) {
      try {
        if (file.isDirectory()) result.addAll(classesInDirectory(null, file));
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    if (Play.configuration.containsKey(SCAN_JARS_PROP)) {
      String scanJars = Play.configuration.getProperty(SCAN_JARS_PROP);
      if (!scanJars.isEmpty()) {
        logger.trace("{} is not empty, lets scan it: {}", SCAN_JARS_PROP, scanJars);
        long start = nanoTime();
        try (ScanResult scanResult =  new ClassGraph()
            .enableClassInfo()
            .disableDirScanning()
            .acceptJars(scanJars.split(","))
            .scan()
        ) {
          logger.debug("Scanning jars took {} ms.",
              NANOSECONDS.toMillis(nanoTime() - start));
          result.addAll(scanResult.getAllClasses().loadClasses());
        }
      }
    }

    return result;
  }

  private List<Class<?>> classesInDirectory(String packageName, File directory)
      throws ClassNotFoundException {
    if (directory.getAbsolutePath().contains("/test")) return emptyList();
    if (directory.getAbsolutePath().contains("pdf/build/thirdParty"))
      return emptyList(); // it causes initialisation of org.xhtmlrenderer.swing.AWTFSImage which is slow

    List<Class<?>> result = new ArrayList<>();
    var files = directory.listFiles();
    if (files == null) return result;
    for (File file : files) {
      if (file.isDirectory()) {
        String subPackage =
            packageName == null ? file.getName() : packageName + '.' + file.getName();
        result.addAll(classesInDirectory(subPackage, file));
      } else if (file.getName().endsWith(".class")) {
        String className =
            packageName == null ? classNameOf(file) : packageName + '.' + classNameOf(file);
        result.add(Class.forName(className, false, Thread.currentThread().getContextClassLoader()));
      }
    }
    return result;
  }

  private String classNameOf(File file) {
    return file.getName().replace(".class", "");
  }
}
