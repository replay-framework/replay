package play.rebel;

import play.exceptions.UnexpectedException;
import play.mvc.Controller;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class Bridge {
  static String template() {
    try {
      Method method = Controller.class.getDeclaredMethod("template");
      method.setAccessible(true);
      return (String) method.invoke(null);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }

  static String template(String templateName) {
    try {
      Method method = Controller.class.getDeclaredMethod("template", String.class);
      method.setAccessible(true);
      return (String) method.invoke(null, templateName);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }

  static <T extends Annotation> T getControllerInheritedAnnotation(Class<T> annotationClass) {
    try {
      Method method = Controller.class.getDeclaredMethod("getControllerInheritedAnnotation", Class.class);
      method.setAccessible(true);
      return (T) method.invoke(null, annotationClass);
    }
    catch (RuntimeException e) {
      throw e;
    }
    catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }
  
  static void setControllerField(String fieldName, Object fieldValue) throws Exception {
    Field field = Controller.class.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, fieldValue);
  }
}
