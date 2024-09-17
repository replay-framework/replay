package play.utils;

import static java.util.Collections.sort;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.mvc.With;

/** Java utils */
public class Java {

  protected static JavaWithCaching _javaWithCaching = new JavaWithCaching();

  protected static JavaWithCaching getJavaWithCaching() {
    return _javaWithCaching;
  }

  /**
   * Retrieve parameter names of a method
   *
   * @param method The given method
   * @return Array of parameter names
   */
  public static String[] parameterNames(Method method) {
    Parameter[] parameters = method.getParameters();
    String[] names = new String[parameters.length];
    for (int i = 0; i < parameters.length; i++) {
      names[i] = parameters[i].getName();
    }
    return names;
  }

  /**
   * Find all annotated method from a class
   *
   * @param clazz The class
   * @param annotationType The annotation class
   * @return A list of method object
   */
  public static List<Method> findAllAnnotatedMethods(
      Class<?> clazz, Class<? extends Annotation> annotationType) {
    return getJavaWithCaching().findAllAnnotatedMethods(clazz, annotationType);
  }

  public static byte[] serialize(Object o) throws IOException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      try (ObjectOutputStream oo = new ObjectOutputStream(baos)) {
        oo.writeObject(o);
        oo.flush();
      }
      return baos.toByteArray();
    }
  }

  public static Object deserialize(byte[] b) throws IOException, ClassNotFoundException {
    try (ByteArrayInputStream bais = new ByteArrayInputStream(b)) {
      try (ObjectInputStream oi = new ObjectInputStream(bais)) {
        return oi.readObject();
      }
    }
  }
}

/**
 * This is an internal class uses only by the Java-class. It contains functionality with caching..
 *
 * <p>The idea is that the Java-objects creates a new instance of JavaWithCaching, each time
 * something new is compiled..
 */
class JavaWithCaching {

  /** Class uses as key for storing info about the relation between a Class and an Annotation */
  private static class ClassAndAnnotation {
    private final Class<?> clazz;
    private final Class<? extends Annotation> annotation;

    private ClassAndAnnotation(Class<?> clazz, Class<? extends Annotation> annotation) {
      this.clazz = clazz;
      this.annotation = annotation;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      ClassAndAnnotation that = (ClassAndAnnotation) o;

      if (annotation != null ? !annotation.equals(that.annotation) : that.annotation != null)
        return false;
      return clazz != null ? clazz.equals(that.clazz) : that.clazz == null;
    }

    @Override
    public int hashCode() {
      int result = clazz != null ? clazz.hashCode() : 0;
      result = 31 * result + (annotation != null ? annotation.hashCode() : 0);
      return result;
    }
  }

  // cache follows..

  private final Object classAndAnnotationsLock = new Object();
  private final Map<ClassAndAnnotation, List<Method>> classAndAnnotation2Methods = new HashMap<>();
  private final Map<Class<?>, List<Method>> class2AllMethodsWithAnnotations = new HashMap<>();

  /**
   * Find all annotated method from a class
   *
   * @param clazz The class
   * @param annotationType The annotation class
   * @return A list of method object
   */
  public List<Method> findAllAnnotatedMethods(
      Class<?> clazz, Class<? extends Annotation> annotationType) {

    if (clazz == null) {
      return new ArrayList<>(0);
    }

    synchronized (classAndAnnotationsLock) {

      // first look in cache

      ClassAndAnnotation key = new ClassAndAnnotation(clazz, annotationType);

      List<Method> methods = classAndAnnotation2Methods.get(key);
      if (methods != null) {
        // cache hit
        return methods;
      }
      // have to resolve it.
      methods = new ArrayList<>();

      // get list of all annotated methods on this class..
      for (Method method : findAllAnnotatedMethods(clazz)) {
        if (method.isAnnotationPresent(annotationType)) {
          methods.add(method);
        }
      }

      sortByPriority(methods, annotationType);

      // store it in cache
      classAndAnnotation2Methods.put(key, methods);

      return methods;
    }
  }

  private void sortByPriority(
      List<Method> methods, final Class<? extends Annotation> annotationType) {
    try {
      final Method priority = annotationType.getMethod("priority");
      sort(
          methods,
          (m1, m2) -> {
            try {
              Integer priority1 = (Integer) priority.invoke(m1.getAnnotation(annotationType));
              Integer priority2 = (Integer) priority.invoke(m2.getAnnotation(annotationType));
              return priority1.compareTo(priority2);
            } catch (Exception e) {
              // should not happen
              throw new RuntimeException(e);
            }
          });
    } catch (NoSuchMethodException e) {
      // no need to sort - this annotation doesn't have priority() method
    }
  }

  /**
   * Find all annotated method from a class
   *
   * @param clazz The class
   * @return A list of method object
   */
  public List<Method> findAllAnnotatedMethods(Class<?> clazz) {
    synchronized (classAndAnnotationsLock) {
      // first check the cache..
      List<Method> methods = class2AllMethodsWithAnnotations.get(clazz);
      if (methods != null) {
        // cache hit
        return methods;
      }
      // have to resolve it..
      methods = new ArrayList<>();
      // Clazz can be null if we are looking at an interface / annotation
      while (clazz != null && !clazz.equals(Object.class)) {
        for (Method method : clazz.getDeclaredMethods()) {
          if (method.getAnnotations().length > 0) {
            methods.add(method);
          }
        }
        if (clazz.isAnnotationPresent(With.class)) {
          for (Class withClass : clazz.getAnnotation(With.class).value()) {
            methods.addAll(findAllAnnotatedMethods(withClass));
          }
        }
        clazz = clazz.getSuperclass();
      }

      // store it in the cache.
      class2AllMethodsWithAnnotations.put(clazz, methods);
      return methods;
    }
  }
}
