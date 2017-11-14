package play.modules.gtengineplugin.gt_integration;

import play.Play;
import play.classloading.ApplicationClassloaderState;
import play.template2.compile.GTJavaExtensionMethodResolver;
import play.templates.JavaExtensions;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GTJavaExtensionMethodResolver1x implements GTJavaExtensionMethodResolver {

    private static Object lock = new Object();
    private static ApplicationClassloaderState _lastKnownApplicationClassloaderState;
    private static Map<String, Class> methodName2ClassMapping;

    @Override 
    public Class findClassWithMethod(String methodName) {
        synchronized (lock) {
            if (_lastKnownApplicationClassloaderState == null || !_lastKnownApplicationClassloaderState.equals(Play.classloader.currentState) || methodName2ClassMapping == null) {
                _lastKnownApplicationClassloaderState = Play.classloader.currentState;
                List<Class> extensionsClassnames = new ArrayList<>(5);
                extensionsClassnames.add(JavaExtensions.class);
                extensionsClassnames.addAll(Play.classloader.getAssignableClasses(JavaExtensions.class));

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
