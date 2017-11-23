package play.classloading;

import play.Play;

import java.lang.annotation.Annotation;
import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang.StringUtils.replace;

/**
 * The application classLoader. Load the classes from the application Java sources files.
 */
public class ApplicationClassloader {
    JavaClassesScanner scanner = new JavaClassesScanner();

    public List<Class<?>> getAllClasses() {
        if (allClasses == null) {
            List<Class<?>> result = scanner.allClassesInProject();

            Play.classes.set(result);

            result.sort(comparing(Class::getName));

            Map<String, Class> byNormalizedName = new HashMap<>(result.size());
            for (Class clazz : result) {
                byNormalizedName.put(clazz.getName().toLowerCase(), clazz);
                if (clazz.getName().contains("$")) {
                    byNormalizedName.put(replace(clazz.getName().toLowerCase(), "$", "."), clazz);
                }
            }

            allClassesByNormalizedName = unmodifiableMap(byNormalizedName);
            allClasses = unmodifiableList(result);
        }
        return allClasses;
    }

    private List<Class<?>> allClasses;
    private Map<String, Class> allClassesByNormalizedName;
    private final Map<String, List<Class>> assignableClassesByName = new HashMap<>(100);

    /**
     * Retrieve all application classes assignable to this class.
     * 
     * @param clazz
     *            The superclass, or the interface.
     * @return A list of class
     */
    public List<Class> getAssignableClasses(Class<?> clazz) {
        if (clazz == null) {
            return Collections.emptyList();
        }
        getAllClasses();
        List<Class> results = assignableClassesByName.get(clazz.getName());
        if (results == null) {
            results = unmodifiableList(Play.classes.getAssignableClasses(clazz));
            assignableClassesByName.put(clazz.getName(), results); // cache assignable classes
        }
        return results;
    }

    public Class<?> getClassIgnoreCase(String name) {
        getAllClasses();
        String nameLowerCased = name.toLowerCase();
        return allClassesByNormalizedName.get(nameLowerCased);
    }

    /**
     * Retrieve all application classes with a specific annotation.
     * 
     * @param clazz
     *            The annotation class.
     * @return A list of class
     */
    public List<Class> getAnnotatedClasses(Class<? extends Annotation> clazz) {
        getAllClasses();
        return Play.classes.getAnnotatedClasses(clazz);
    }
}
