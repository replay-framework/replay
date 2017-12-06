package play.data.binding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.exceptions.UnexpectedException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Parameters map to POJO binder.
 */
public class BeanWrapper {
    private static final Logger logger = LoggerFactory.getLogger(BeanWrapper.class);

    static final int notwritableField = Modifier.FINAL | Modifier.NATIVE | Modifier.STATIC;
    static final int notaccessibleMethod = Modifier.NATIVE | Modifier.STATIC;

    private Class<?> beanClass;

    /** 
     * a cache for our properties and setters
     */
    private Map<String, Property> wrappers = new HashMap<>();

    public BeanWrapper(Class<?> forClass) {
        logger.trace("Bean wrapper for class {}", forClass.getName());

        this.beanClass = forClass;

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
        String message = String.format("Can't find property with name '%s' on class %s", name, instance.getClass().getName());
        logger.warn(message);
        throw new UnexpectedException(message);

    }

    private boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getName().length() > 3 &&
          method.getParameterTypes().length == 1 && (method.getModifiers() & notaccessibleMethod) == 0;
    }

    protected Object newBeanInstance() throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Constructor<?> constructor = beanClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
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
            String propertyName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
            Property wrapper = new Property(propertyName, method);
            wrappers.put(propertyName, wrapper);
        }
    }

    public static class Property {

        private Annotation[] annotations;
        private Method setter;
        private Field field;
        private Class<?> type;
        private Type genericType;
        private String name;

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
                logger.warn("ERROR in BeanWrapper when setting property {} value is {} ({})", name, value, value == null ? null : value.getClass(), ex);
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

    public Object bind(String name, Type type, Map<String, String[]> params, String prefix, Annotation[] annotations) throws Exception {
        Object instance = newBeanInstance();
        return bind(name, type, params, prefix, instance, annotations);
    }

    public Object bind(String name, Type type, Map<String, String[]> params, String prefix, Object instance, Annotation[] annotations) {
        RootParamNode paramNode = RootParamNode.convert( params);
        // when looking at the old code in BeanBinder and Binder.bindInternal, I
        // think it is correct to use 'name+prefix'
        Binder.bindBean( paramNode.getChild(name+prefix), instance, annotations);
        return instance;
    }
}
