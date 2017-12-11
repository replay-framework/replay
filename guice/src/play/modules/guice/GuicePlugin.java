package play.modules.guice;

import com.google.inject.*;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.PlayPlugin;
import play.inject.BeanSource;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Enable <a href="http://google-guice.googlecode.com">Guice</a> integration in
 * Playframework. This plugin first scans for a custom Guice Injector if it's
 * not found, then it tries to create an injector from all the guice modules
 * available on the classpath. The Plugin is then passed to Play injector for
 * Controller IoC.
 *
 * @author <a href="mailto:info@lucianofiandesio.com">Luciano Fiandesio</a>
 * @author <a href="mailto:info@hausel@freemail.hu">Peter Hausel</a>
 * @author <a href="mailto:lrgalego@gmail.com">Lucas Galego</a>
 * @author <a href="mailto:a.a.vasiljev@gmail.com">Alexander Vasiljev</a>
 */
public class GuicePlugin extends PlayPlugin implements BeanSource {
  private static final Logger logger = LoggerFactory.getLogger(GuicePlugin.class);
  
  Injector injector;
  private final List<Module> modules = new ArrayList<Module>();

  @Override
  public void onApplicationStart() {
    logger.debug("Starting Guice modules injecting");
    loadInjector();
    play.inject.Injector.setBeanSource(this);
    play.inject.Injector.inject(this);
    injectAnnotated();
    injectPlayPlugins();
  }

  private void loadInjector() {
    try {
      modules.clear();
      logger.debug("Guice modules cleared");
      List<Class<? extends GuiceSupport>> customInjectors = Play.classes.getAssignableClasses(GuiceSupport.class);
      if (!customInjectors.isEmpty()) {
        if (customInjectors.size() > 1) {
          logger.warn("Found multiple customer injectors: {}, using first of them", customInjectors);
        }
        loadCustomInjector(customInjectors.get(0));
        return;
      }

      List<Class<? extends AbstractModule>> modulesClasses = Play.classes.getAssignableClasses(AbstractModule.class);
      for (Class moduleClass : modulesClasses) {
        modules.add((Module) moduleClass.newInstance());
      }
      loadInjectorFromModules();
    }
    catch (Exception e) {
      throw new IllegalStateException("Unable to create Guice injector", e);
    }
  }

  private void loadCustomInjector(Class clazz) throws InstantiationException, IllegalAccessException {
    final GuiceSupport gs = (GuiceSupport) clazz.newInstance();
    injector = gs.configure();
    logger.info("Guice injector created: {}", clazz.getName());
  }

  private void loadInjectorFromModules() {
    if (modules.isEmpty()) {
      throw new IllegalStateException("Could not find any custom guice injector or abstract modules. Are you sure you have at least one on the classpath?");
    }
    injector = Guice.createInjector(modules);
    logger.info("Guice injector created with modules: " + moduleList());
  }

  private String moduleList() {
    final StringBuilder moduleList = new StringBuilder("\n");
    for (Module module : modules) {
      moduleList.append(module.getClass());
      moduleList.append("\n");
    }
    return moduleList.toString();
  }

  @Override
  public <T> T getBeanOfType(Class<T> clazz) {
    if (injector == null) {
      return null;
    }
    try {
      return injector.getInstance(clazz);
    }
    catch (ConfigurationException e) {
      logger.error("Failed to get bean of type " + clazz, e);
      return null;
    }
  }

  public <T> T getBeanWithKey(Key<T> key) {
    if (injector == null) {
      return null;
    }
    return injector.getInstance(key);
  }

  private void injectAnnotated() {
    try {
      for (Class<?> clazz : Play.classes.getAnnotatedClasses(play.modules.guice.InjectSupport.class)) {
        for (Field field : clazz.getDeclaredFields()) {
          if (isInjectable(field)) {
            inject(field);
          }
        }
      }
    }
    catch (Exception e) {
      throw new RuntimeException("Error injecting dependencies", e);
    }
  }

  private void injectPlayPlugins() {
    for (PlayPlugin plugin : Play.pluginCollection.getAllPlugins()) {
      injector.injectMembers(plugin);
    }
  }

  private boolean isInjectable(Field field) {
    return Modifier.isStatic(field.getModifiers()) && 
        (field.isAnnotationPresent(javax.inject.Inject.class) || field.isAnnotationPresent(com.google.inject.Inject.class));
  }

  private void inject(Field field) throws IllegalAccessException {
    field.setAccessible(true);
    final Annotation fieldBinding = fieldBinding(field);
    if (fieldBinding != null) {
      field.set(null, getBeanWithKey(Key.get(field.getType(), fieldBinding)));
    }
    else {
      field.set(null, getBeanOfType(field.getType()));
    }
  }

  private Annotation fieldBinding(Field field) {
    for (Annotation annotation : field.getAnnotations()) {
      if (annotation.annotationType().equals(Named.class)) {
        return annotation;
      }
      for (Annotation internal : annotation.annotationType().getAnnotations()) {
        if (internal.annotationType().equals(BindingAnnotation.class)) {
          return annotation;
        }
      }
    }
    return null;
  }

}
