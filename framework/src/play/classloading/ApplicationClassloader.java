package play.classloading;

import play.Play;
import play.classloading.ApplicationClasses.ApplicationClass;

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

            Play.classes.clear();

            for (Class<?> javaClass : result) {
                ApplicationClass appClass = new ApplicationClass(javaClass.getName());
                appClass.javaClass = javaClass;
                Play.classes.add(appClass);
            }

            result.sort(comparing(Class::getName));

            Map<String, ApplicationClass> byNormalizedName = new HashMap<>(result.size());
            for (ApplicationClass clazz : Play.classes.all()) {
                byNormalizedName.put(clazz.name.toLowerCase(), clazz);
                if (clazz.name.contains("$")) {
                    byNormalizedName.put(replace(clazz.name.toLowerCase(), "$", "."), clazz);
                }
            }

            allClassesByNormalizedName = unmodifiableMap(byNormalizedName);
            allClasses = unmodifiableList(result);
        }
        return allClasses;
    }

    private List<Class<?>> allClasses;
    private Map<String, ApplicationClass> allClassesByNormalizedName;
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
        if (results != null) {
            return results;
        } else {
            results = new ArrayList<>();
            for (ApplicationClass c : Play.classes.getAssignableClasses(clazz)) {
                results.add(c.javaClass);
            }
            // cache assignable classes
            assignableClassesByName.put(clazz.getName(), unmodifiableList(results));
        }
        return results;
    }

    public Class<?> getClassIgnoreCase(String name) {
        getAllClasses();
        String nameLowerCased = name.toLowerCase();
        ApplicationClass c = allClassesByNormalizedName.get(nameLowerCased);
        return c != null ? c.javaClass : null;
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
        List<Class> results = new ArrayList<>();
        for (ApplicationClass c : Play.classes.getAnnotatedClasses(clazz)) {
            results.add(c.javaClass);
        }
        return results;
    }
}
