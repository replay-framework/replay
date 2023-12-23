package play;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.cache.Cache;
import play.classloading.ApplicationClasses;
import play.inject.BeanSource;
import play.inject.DefaultBeanSource;
import play.inject.Injector;
import play.jobs.Job;
import play.mvc.ActionInvoker;
import play.mvc.CookieSessionStore;
import play.mvc.PlayController;
import play.mvc.Router;
import play.mvc.SessionStore;
import play.plugins.PluginCollection;
import play.templates.FastTags;
import play.templates.JavaExtensions;
import play.templates.TemplateLoader;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Main framework class
 */
public class Play {
  private static final Logger logger = LoggerFactory.getLogger(Play.class);

  public enum Mode {

    /**
     * Enable development-specific features, e.g. view the documentation at the URL {@literal "/@documentation"}.
     */
    DEV,
    /**
     * Disable development-specific features.
     */
    PROD;

    public boolean isDev() {
      return this == DEV;
    }

    public boolean isProd() {
      return this == PROD;
    }
  }

  public static boolean started;
  public static String id = System.getProperty("play.id", "");
  public static Mode mode = Mode.DEV;
  public static File appRoot = new File(System.getProperty("user.dir"));
  public static File tmpDir;
  public static final ApplicationClasses classes = new ApplicationClasses();

  /**
   * All paths to search for templates files
   */
  public static List<File> templatesPath = new ArrayList<>(2);
  /**
   * The routes file
   */
  public static ClasspathResource routes;

  /**
   * The app configuration (already resolved from the framework id)
   */
  public static Properties configuration = new Properties();
  /**
   * The last time than the application has started
   */
  public static long startedAt;
  /**
   * The list of supported locales
   */
  public static List<String> langs = new ArrayList<>(16);
  /**
   * The very secret key
   */
  public static String secretKey;
  /**
   * pluginCollection that holds all loaded plugins and all enabled plugins
   */
  public static PluginCollection pluginCollection = new PluginCollection();

  public static boolean usePrecompiled;

  /**
   * This is used as default encoding everywhere related to the web: request, response, WS
   */
  public static final Charset defaultWebEncoding = UTF_8;

  public static Invoker invoker;

  /**
   * Not recommended to use directly. 
   * Instead, you should use @Inject annotation to inject your dependencies (either constructor or field injection).
   */
  public static BeanSource beanSource;

  private final ConfLoader confLoader;

  private final ActionInvoker actionInvoker;

  public Play() {
    this(new DefaultBeanSource());
  }

  public Play(BeanSource beanSource) {
    this(new PropertiesConfLoader(), beanSource, new CookieSessionStore());
  }

  public Play(ConfLoader confLoader, BeanSource beanSource, SessionStore sessionStore) {
    Play.beanSource = beanSource;
    this.confLoader = confLoader;
    this.actionInvoker = new ActionInvoker(sessionStore);
  }

  /**
   * Init the framework
   *
   * @param id The framework id to use
   */
  public void init(String id) {
    setupBase(id);
    readConfiguration();
    new PlayLoggingSetup().init();
    logger.info("Starting {}", appRoot.getAbsolutePath());
    setupTmpDir();
    setupApplicationMode();
    setupAppRoot();
    routes = ClasspathResource.file("routes");
    pluginCollection.loadPlugins();
    Play.invoker = new Invoker();
  }

  /**
   * Minimalistic initialization of the framework; allowing DIY-initialization
   * (no modules, no plugins from .plugins file, no routes from routes file, no config file)
   *
   * @param id The framework id to use
   */
  public void minimalInit(String id) {
    setupBase(id);
    new PlayLoggingSetup().init();
    logger.info("Starting {}", appRoot.getAbsolutePath());
    setupTmpDir();
    setupApplicationMode();
    setupAppRoot();

    Play.invoker = new Invoker();
  }

  private void setupBase(String id) {
    Injector.setBeanSource(beanSource);
    Play.usePrecompiled = "true".equals(System.getProperty("precompiled", "false"));
    Play.id = id;
    Play.started = false;
  }

  private static void setupTmpDir() {
    if (configuration.getProperty("play.tmp", "tmp").equals("none")) {
      tmpDir = null;
      logger.debug("No tmp folder will be used (play.tmp is set to none)");
    }
    else {
      tmpDir = new File(configuration.getProperty("play.tmp", "tmp"));
      if (!tmpDir.isAbsolute()) {
        tmpDir = new File(appRoot, tmpDir.getPath());
      }

      logger.trace("Using {} as tmp dir", Play.tmpDir);

      if (!tmpDir.exists()) {
        if (!tmpDir.mkdirs()) {
          throw new IllegalStateException("Could not create " + tmpDir.getAbsolutePath());
        }
      }
    }
  }

  private static void setupApplicationMode() {
    try {
      String configuredMode = configuration.getProperty("application.mode");
      if (configuredMode == null) {
        boolean isDebug = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
        configuredMode = isDebug ? "DEV" : "PROD";
      }
      mode = Mode.valueOf(configuredMode.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format("Illegal mode '%s', use either prod or dev", configuration.getProperty("application.mode")), e);
    }

    // Set to the Prod mode must be done before loadModules call as some modules (e.g. DocViewer) is only available in DEV
    if (usePrecompiled) {
      mode = Mode.PROD;
    }
  }

  private static void setupAppRoot() {
    // Build basic templates path
    templatesPath.clear();
    if (new File(appRoot, "app/views").exists() || (usePrecompiled && new File(appRoot, "precompiled/templates/app/views").exists())) {
      templatesPath.add(new File(appRoot, "app/views"));
    }
  }

  public ActionInvoker getActionInvoker() {
    return actionInvoker;
  }

  /**
   * Read application.conf and resolve overridden key using the play id mechanism.
   */
  private void readConfiguration() {
    configuration = confLoader.readConfiguration(Play.id);
    pluginCollection.onConfigurationRead();
  }

  /**
   * Start the application
   *
   * @throws IllegalArgumentException if the application is already started
   */
  public synchronized void start() {
    if (started) {
      throw new IllegalArgumentException("Play is already started");
    }

    try {
      registerShutdownHook();
      readConfiguration();
      initLangs();
      TemplateLoader.cleanCompiledCache();
      initSecretKey();
      Router.detectChanges();
      Cache.init();
      pluginCollection.onApplicationStart();
      injectStaticFields();

      started = true;
      startedAt = System.currentTimeMillis();
      logger.info("Application '{}' is now started !", configuration.getProperty("application.name", ""));

      pluginCollection.afterApplicationStart();
    }
    catch (RuntimeException e) {
      stop();
      started = false;
      throw e;
    }
  }

  private void injectStaticFields() {
    injectStaticFields(Play.classes.getAssignableClasses(PlayController.class));
    injectStaticFields(Play.classes.getAssignableClasses(Job.class));
    injectStaticFields(Play.classes.getAssignableClasses(FastTags.class));
    injectStaticFields(Play.classes.getAssignableClasses(JavaExtensions.class));
  }

  private <T> void injectStaticFields(List<Class<? extends T>> classes) {
    for (Class<?> clazz : classes) {
      injectStaticFields(clazz);
    }
  }

  private void injectStaticFields(Class<?> clazz) {
    for (Field field : clazz.getDeclaredFields()) {
      if (isStaticInjectable(field)) {
        inject(field);
      }
    }
  }

  private boolean isStaticInjectable(Field field) {
    return Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(Inject.class);
  }

  private void inject(Field field) {
    field.setAccessible(true);
    try {
      field.set(null, beanSource.getBeanOfType(field.getType()));
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Can only register shutdown-hook if running as standalone server
   * registers shutdown hook - Now there's a good chance that we can notify
   * our plugins that we're going down when some calls ctrl+c or just kills our process
   */
  private void registerShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
  }

  private static void initLangs() {
    langs = new ArrayList<>(Arrays.asList(configuration.getProperty("application.langs", "").split(",")));
    if (langs.size() == 1 && langs.get(0).trim().length() == 0) {
      langs = new ArrayList<>(16);
    }
  }

  private static void initSecretKey() {
    secretKey = configuration.getProperty("application.secret", "").trim();
    if (secretKey.isEmpty()) {
      logger.warn("No secret key defined. Sessions will not be encrypted");
    }
  }

  private void stop() {
    if (started) {
      logger.info("Stopping the play application");
      pluginCollection.onApplicationStop();
      started = false;
      Cache.stop();
      Router.lastLoading = 0L;
    }
    else {
      logger.warn("Cannot stop because Play is not started");
    }
  }

  static void detectChanges() {
    Router.detectChanges();
    pluginCollection.detectChange();
  }

  public static <T extends PlayPlugin> T plugin(Class<T> clazz) {
    return pluginCollection.getPluginInstance(clazz);
  }

  /**
   * Search a File in the application
   *
   * @param path Relative path from the applications root
   * @return The virtualFile or null
   */
  @Nullable
  public static File file(String path) {
    File file = new File(appRoot, path);
    return file.exists() ? file : null;
  }

  /**
   * Search a File in the current application
   *
   * @param path Relative path from the application root
   * @return The file even if it doesn't exist
   */
  public static File getFile(String path) {
    return new File(appRoot, path);
  }

  /**
   * Returns true if application is running in test-mode. Test-mode is resolved from the framework id.
   * <p>
   * Your app is running in test-mode if the framework id (Play.id) is 'test' or 'test-?.*'
   *
   * @return true if test mode
   */
  public static boolean runningInTestMode() {
    return id.matches("test|test-?.*");
  }

  public static boolean useDefaultMockMailSystem() {
    return "mock".equals(configuration.getProperty("mail.smtp", "")) && mode == Mode.DEV;
  }

  public static String relativePath(File file) {
    return file.getAbsolutePath().replace(appRoot.getAbsolutePath(), "");
  }
}
