package play;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The class/method that will be invoked by the current operation */
public class InvocationContext {

  public static final ThreadLocal<InvocationContext> current = new ThreadLocal<>();
  private final List<Annotation> annotations;
  private final String invocationType;

  public static InvocationContext current() {
    return current.get();
  }

  public InvocationContext(String invocationType, Annotation[] annotations) {
    this.invocationType = invocationType;
    this.annotations = Arrays.asList(annotations);
  }

  public InvocationContext(String invocationType, Annotation[]... annotations) {
    this.invocationType = invocationType;
    this.annotations = new ArrayList<>();
    for (Annotation[] some : annotations) {
      this.annotations.addAll(Arrays.asList(some));
    }
  }

  public List<Annotation> getAnnotations() {
    return annotations;
  }

  @SuppressWarnings("unchecked")
  public <T extends Annotation> T getAnnotation(Class<T> clazz) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().isAssignableFrom(clazz)) {
        return (T) annotation;
      }
    }
    return null;
  }

  public <T extends Annotation> boolean isAnnotationPresent(Class<T> clazz) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().isAssignableFrom(clazz)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("InvocationType: ");
    builder.append(invocationType);
    builder.append(". annotations: ");
    for (Annotation annotation : annotations) {
      builder.append(annotation.toString()).append(",");
    }
    return builder.toString();
  }
}
