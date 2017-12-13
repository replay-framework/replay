package play.classloading;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.replace;

public class ApplicationClasses {
    /**
     * Cache of all compiled classes
     */
    private Map<String, Class> classes;
    private Map<String, Class> allClassesByNormalizedName;
    private final Map<Class<?>, Object> assignableClasses = new HashMap<>(100);

    private Map<String, Class> classes() {
        if (classes == null) {
            List<Class<?>> applicationClasses = new JavaClassesScanner().allClassesInProject();
            allClassesByNormalizedName = unmodifiableMap(normalizeByName(applicationClasses));
            classes = toMapIgnoringDuplicates(applicationClasses);
        }
        return classes;
    }

    private Map<String, Class> toMapIgnoringDuplicates(List<Class<?>> applicationClasses) {
        BinaryOperator<Class> ignoreDuplicates = (old, _new) -> old;
        return applicationClasses.stream().collect(toMap(cl -> cl.getName(), cl -> cl, ignoreDuplicates));
    }

    private Map<String, Class> normalizeByName(List<Class<?>> applicationClasses) {
        Map<String, Class> byNormalizedName = new HashMap<>(applicationClasses.size());
        for (Class clazz : applicationClasses) {
            byNormalizedName.put(clazz.getName().toLowerCase(), clazz);
            if (clazz.getName().contains("$")) {
                byNormalizedName.put(replace(clazz.getName().toLowerCase(), "$", "."), clazz);
            }
        }
        return byNormalizedName;
    }

    public <T> List<Class<? extends T>> getAssignableClasses(Class<T> aClass) {
        if (aClass == null) {
            return emptyList();
        }

        List<Class<? extends T>> result = (List<Class<? extends T>>) assignableClasses.get(aClass);
        if (result == null) {
            result = findAssignableClasses(aClass);
            assignableClasses.put(aClass, result);
        }
        return result;
    }

    private <T> List<Class<? extends T>> findAssignableClasses(Class<T> clazz) {
        return unmodifiableList(classes().values().stream()
          .filter(applicationClass -> isSubclass(applicationClass, clazz))
          .map(applicationClass -> (Class<T>) applicationClass)
          .collect(toList()));
    }

    private boolean isSubclass(Class<?> subClass, Class<?> superClass) {
        return superClass.isAssignableFrom(subClass) && !subClass.equals(superClass);
    }

    public Class<?> getClassIgnoreCase(String name) {
        String nameLowerCased = name.toLowerCase();
        return allClassesByNormalizedName.get(nameLowerCased);
    }

    /**
     * Get a class by name
     * 
     * @param name
     *            The fully qualified class name
     * @return The ApplicationClass or null
     */
    public Class getApplicationClass(String name) {
        return classes().get(name);
    }

    /**
     * Retrieve all application classes with a specific annotation.
     *
     * @param clazz
     *            The annotation class.
     * @return A list of application classes.
     */
    public List<Class> getAnnotatedClasses(Class<? extends Annotation> clazz) {
        return classes().values().stream().filter(applicationClass ->
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
        return classes().containsKey(name);
    }

    @Override
    public String toString() {
        return String.format("%s classes", classes().size());
    }
}
