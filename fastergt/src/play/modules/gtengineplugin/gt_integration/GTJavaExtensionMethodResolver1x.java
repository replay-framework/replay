package play.modules.gtengineplugin.gt_integration;

import play.Play;
import play.template2.compile.GTJavaExtensionMethodResolver;
import play.template2.JavaExtensions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GTJavaExtensionMethodResolver1x implements GTJavaExtensionMethodResolver {

    private static final Object lock = new Object();
    private static Map<String, Class> methodName2ClassMapping;

    @Override 
    public Class findClassWithMethod(String methodName) {
        synchronized (lock) {
            if (methodName2ClassMapping == null) {
                List<Class> extensionsClassnames = new ArrayList<>(5);
                extensionsClassnames.add(JavaExtensions.class);
                extensionsClassnames.addAll(Play.classes.getAssignableClasses(JavaExtensions.class));

                methodName2ClassMapping = new HashMap<>();
                for ( Class clazz : extensionsClassnames) {
                    for ( Method method : clazz.getDeclaredMethods()) {
                        methodName2ClassMapping.put(method.getName(), clazz);
                    }
                }
            }
        }

        return methodName2ClassMapping.get(methodName);

    }
}
