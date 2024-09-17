package play.data.binding;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Scope;

/** Parameters map to POJO binder. */
public class BeanWrapper {
  private static final Logger logger = LoggerFactory.getLogger(BeanWrapper.class);

  static final int notwritableField = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;
  private static final int notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;

  /** a cache for our properties and setters */
  private final Map<String, Property> wrappers = new HashMap<>();

  public BeanWrapper(Class<?> forClass) {
    logger.trace("Bean wrapper for class {}", forClass.getName());

    registerSetters(forClass);
    registerFields(forClass);
  }

  public Collection<Property> getWrappers() {
    return wrappers.values();
  }

  public void set(String name, Object instance, Object value) {
    for (Property prop : wrappers.values()) {
      if (name.equals(prop.name)) {
        prop.setValue(instance, value);
        return;
      }
    }
    String message =
        String.format(
            "Can't find property with name '%s' on class %s", name, instance.getClass().getName());
    logger.warn(message);
    throw new UnexpectedException(message);
  }

  private boolean isSetter(Method method) {
    return method.getName().startsWith("set")
        && method.getName().length() > 3
        && method.getParameterTypes().length == 1
        && (method.getModifiers() & notaccessibleMethod) == 0;
  }

  private void registerFields(Class<?> clazz) {
    // recursive stop condition
    if (clazz == Object.class) {
      return;
    }
    Field[] fields = clazz.getFields();
    for (Field field : fields) {
      if (wrappers.containsKey(field.getName())) {
        continue;
      }
      if ((field.getModifiers() & notwritableField) != 0) {
        continue;
      }
      Property w = new Property(field);
      wrappers.put(field.getName(), w);
    }
    registerFields(clazz.getSuperclass());
  }

  private void registerSetters(Class<?> clazz) {
    if (clazz == Object.class) {
      return;
      // deep walk (superclass first)
    }
    registerSetters(clazz.getSuperclass());

    Method[] methods = clazz.getDeclaredMethods();
    for (Method method : methods) {
      if (!isSetter(method)) {
        continue;
      }
      String propertyName =
          method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
      Property wrapper = new Property(propertyName, method);
      wrappers.put(propertyName, wrapper);
    }
  }

  public static class Property {

    private final Annotation[] annotations;
    private Method setter;
    private Field field;
    private final Class<?> type;
    private final Type genericType;
    private final String name;

    Property(String propertyName, Method setterMethod) {
      name = propertyName;
      setter = setterMethod;
      type = setter.getParameterTypes()[0];
      annotations = setter.getAnnotations();
      genericType = setter.getGenericParameterTypes()[0];
    }

    Property(Field field) {
      this.field = field;
      this.field.setAccessible(true);
      name = field.getName();
      type = field.getType();
      annotations = field.getAnnotations();
      genericType = field.getGenericType();
    }

    public void setValue(Object instance, Object value) {
      try {
        if (setter != null) {
          logger.trace("invoke setter {} on {} with value {}", setter, instance, value);
          setter.invoke(instance, value);
        } else {
          logger.trace("field.set({}, {})", instance, value);

          field.set(instance, value);
        }

      } catch (Exception ex) {
        logger.warn(
            "ERROR in BeanWrapper when setting property {} value is {} ({})",
            name,
            value,
            value == null ? null : value.getClass(),
            ex);
        throw new UnexpectedException(ex);
      }
    }

    String getName() {
      return name;
    }

    Class<?> getType() {
      return type;
    }

    Type getGenericType() {
      return genericType;
    }

    Annotation[] getAnnotations() {
      return annotations;
    }

    @Override
    public String toString() {
      return type + "." + name;
    }
  }

  public Object bind(
      Http.Request request,
      Scope.Session session,
      String name,
      Map<String, String[]> params,
      String prefix,
      Object instance,
      Annotation[] annotations) {
    RootParamNode paramNode = ParamNode.convert(params);
    // when looking at the old code in BeanBinder and Binder.bindInternal, I
    // think it is correct to use 'name+prefix'
    Binder.bindBean(request, session, paramNode.getChild(name + prefix), instance, annotations);
    return instance;
  }
}
