package play.classloading;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Application classes container.
 */
public class ApplicationClasses {
    /**
     * Cache of all compiled classes
     */
    private final Map<String, Class> classes = new HashMap<>();

    void set(List<Class<?>> applicationClasses) {
        for (Class<?> applicationClass : applicationClasses) {
            classes.put(applicationClass.getName(), applicationClass);
        }
    }

    /**
     * Get a class by name
     * 
     * @param name
     *            The fully qualified class name
     * @return The ApplicationClass or null
     */
    public Class getApplicationClass(String name) {
        return classes.get(name);
    }

    /**
     * Retrieve all application classes assignable to this class.
     * 
     * @param clazz
     *            The superclass, or the interface.
     * @return A list of application classes.
     */
    public List<Class> getAssignableClasses(Class<?> clazz) {
        return classes.values().stream().filter(applicationClass ->
          clazz.isAssignableFrom(applicationClass) && !applicationClass.getName().equals(clazz.getName()))
          .collect(toList());
    }

    /**
     * Retrieve all application classes with a specific annotation.
     * 
     * @param clazz
     *            The annotation class.
     * @return A list of application classes.
     */
    public List<Class> getAnnotatedClasses(Class<? extends Annotation> clazz) {
        return classes.values().stream().filter(applicationClass ->
          applicationClass != null && applicationClass.isAnnotationPresent(clazz))
          .collect(toList());
    }

    /**
     * Does this class is already loaded ?
     * 
     * @param name
     *            The fully qualified class name
     * @return true if the class is loaded
     */
    public boolean hasClass(String name) {
        return classes.containsKey(name);
    }

    @Override
    public String toString() {
        return classes.toString();
    }
}
