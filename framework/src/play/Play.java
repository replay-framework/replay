package play;

import org.slf4j.LoggerFactory;
import play.cache.Cache;
import play.classloading.ApplicationClasses;
import play.libs.IO;
import play.mvc.Http;
import play.mvc.Router;
import play.plugins.PluginCollection;
import play.templates.TemplateLoader;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main framework class
 */
public class Play {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Play.class);

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

    public static boolean initialized;
    public static boolean started;
    public static String id = System.getProperty("play.id", "");
    public static Mode mode = Mode.DEV;
    public static File applicationPath = new File(System.getProperty("user.dir"));
    public static File tmpDir;
    public static final ApplicationClasses classes = new ApplicationClasses();

    /**
     * All paths to search for files
     */
    public static List<VirtualFile> roots = new ArrayList<>(16);
    /**
     * All paths to search for Java files
     */
    public static List<VirtualFile> javaPath = new CopyOnWriteArrayList<>();
    /**
     * All paths to search for templates files
     */
    public static List<VirtualFile> templatesPath = new ArrayList<>(2);
    /**
     * Main routes file
     */
    public static VirtualFile routes;
    /**
     * Plugin routes files
     */
    public static Map<String, VirtualFile> modulesRoutes = new HashMap<>(16);
    /**
     * The loaded configuration files
     */
    public static Set<VirtualFile> confs = new HashSet<>(1);
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
     * pluginCollection that holds all loaded plugins and all enabled plugins..
     */
    public static PluginCollection pluginCollection = new PluginCollection();

    /**
     * Modules
     */
    public static Map<String, VirtualFile> modules = new HashMap<>(16);

    public static boolean usePrecompiled;

    /**
     * This is used as default encoding everywhere related to the web: request, response, WS
     */
    public static String defaultWebEncoding = "utf-8";

    /**
     * Init the framework
     *
     * @param root
     *            The application path
     * @param id
     *            The framework id to use
     */
    public static void init(File root, String id) {
        Play.id = id;
        Play.started = false;
        Play.applicationPath = root;
        readConfiguration();
        Logger.init();

        logger.info("Starting {}", root.getAbsolutePath());

        if (configuration.getProperty("play.tmp", "tmp").equals("none")) {
            tmpDir = null;
            logger.debug("No tmp folder will be used (play.tmp is set to none)");
        } else {
            tmpDir = new File(configuration.getProperty("play.tmp", "tmp"));
            if (!tmpDir.isAbsolute()) {
                tmpDir = new File(applicationPath, tmpDir.getPath());
            }

            logger.trace("Using {} as tmp dir", Play.tmpDir);

            if (!tmpDir.exists()) {
                if (!tmpDir.mkdirs()) {
                    throw new IllegalStateException("Could not create " + tmpDir.getAbsolutePath());
                }
            }
        }

        try {
            mode = Mode.valueOf(configuration.getProperty("application.mode", "DEV").toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.error("Illegal mode '{}', use either prod or dev", configuration.getProperty("application.mode"));
            fatalServerErrorOccurred();
        }

        // Set to the Prod mode must be done before loadModules call as some modules (e.g. DocViewer) is only available in DEV
        if (usePrecompiled || System.getProperty("precompile") != null) {
            mode = Mode.PROD;
        }

        // Build basic java source path
        VirtualFile appRoot = VirtualFile.open(applicationPath);
        roots.clear();
        roots.add(appRoot);

        javaPath.clear();
        javaPath.add(appRoot.child("app"));
        javaPath.add(appRoot.child("conf"));

        // Build basic templates path
        templatesPath.clear();
        if (appRoot.child("app/views").exists() || (usePrecompiled && appRoot.child("precompiled/templates/app/views").exists())) {
            templatesPath.add(appRoot.child("app/views"));
        }

        routes = appRoot.child("conf/routes");
        modulesRoutes.clear();
        loadModules(appRoot);

        // Default cookie domain
        Http.Cookie.defaultDomain = configuration.getProperty("application.defaultCookieDomain", null);
        if (Http.Cookie.defaultDomain != null) {
            logger.info("Using default cookie domain: {}", Http.Cookie.defaultDomain);
        }

        pluginCollection.loadPlugins();
        Play.initialized = true;
    }

    /**
     * Read application.conf and resolve overridden key using the play id mechanism.
     */
    public static void readConfiguration() {
        confs = new HashSet<>();
        configuration = readOneConfigurationFile("application.conf");
        extractHttpPort();
        // Plugins
        pluginCollection.onConfigurationRead();
    }

    private static void extractHttpPort() {
        String javaCommand = System.getProperty("sun.java.command", "");
        jregex.Matcher m = new jregex.Pattern(".* --http.port=({port}\\d+)").matcher(javaCommand);
        if (m.matches()) {
            configuration.setProperty("http.port", m.group("port"));
        }
    }

    private static Properties readOneConfigurationFile(String filename) {
        Properties propsFromFile = null;

        VirtualFile appRoot = VirtualFile.open(applicationPath);

        VirtualFile conf = appRoot.child("conf/" + filename);
        if (confs.contains(conf)) {
            throw new RuntimeException("Detected recursive @include usage. Have seen the file " + filename + " before");
        }

        try {
            propsFromFile = IO.readUtf8Properties(conf.inputstream());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                logger.error("Cannot read {}", filename);
                fatalServerErrorOccurred();
            }
        }
        confs.add(conf);

        // OK, check for instance specifics configuration
        Properties newConfiguration = new OrderSafeProperties();
        Pattern pattern = Pattern.compile("^%([a-zA-Z0-9_\\-]+)\\.(.*)$");
        for (Object key : propsFromFile.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (!matcher.matches()) {
                newConfiguration.put(key, propsFromFile.get(key).toString().trim());
            }
        }
        for (Object key : propsFromFile.keySet()) {
            Matcher matcher = pattern.matcher(key + "");
            if (matcher.matches()) {
                String instance = matcher.group(1);
                if (instance.equals(id)) {
                    newConfiguration.put(matcher.group(2), propsFromFile.get(key).toString().trim());
                }
            }
        }
        propsFromFile = newConfiguration;
        // Resolve ${..}
        pattern = Pattern.compile("\\$\\{([^}]+)}");
        for (Object key : propsFromFile.keySet()) {
            String value = propsFromFile.getProperty(key.toString());
            Matcher matcher = pattern.matcher(value);
            StringBuffer newValue = new StringBuffer(100);
            while (matcher.find()) {
                String jp = matcher.group(1);
                String r = System.getProperty(jp);
                if (r == null) {
                    r = System.getenv(jp);
                }
                if (r == null) {
                    logger.warn("Cannot replace {} in configuration ({}={})", jp, key, value);
                    continue;
                }
                matcher.appendReplacement(newValue, r.replaceAll("\\\\", "\\\\\\\\"));
            }
            matcher.appendTail(newValue);
            propsFromFile.setProperty(key.toString(), newValue.toString());
        }
        // Include
        Map<Object, Object> toInclude = new HashMap<>(16);
        for (Object key : propsFromFile.keySet()) {
            if (key.toString().startsWith("@include.")) {
                try {
                    String filenameToInclude = propsFromFile.getProperty(key.toString());
                    toInclude.putAll(readOneConfigurationFile(filenameToInclude));
                } catch (Exception ex) {
                    logger.warn("Missing include: {}", key, ex);
                }
            }
        }
        propsFromFile.putAll(toInclude);

        return propsFromFile;
    }

    /**
     * Start the application
     *
     * @throws IllegalArgumentException if the application is already started
     */
    public static synchronized void start() {
        if (started) {
            throw new IllegalArgumentException("Play is already started");
        }

        try {
            registerShutdownHook();
            readConfiguration();
            initLangs();
            TemplateLoader.cleanCompiledCache();
            initSecretKey();
            initDefaultWebEncoding();
            Router.detectChanges();
            Cache.init();
            pluginCollection.onApplicationStart();

            started = true;
            startedAt = System.currentTimeMillis();
            logger.info("Application '{}' is now started !", configuration.getProperty("application.name", ""));

            pluginCollection.afterApplicationStart();
        } catch (RuntimeException e) {
            Play.stop();
            started = false;
            throw e;
        }
    }

    /**
     * Can only register shutdown-hook if running as standalone server
     * registers shutdown hook - Now there's a good chance that we can notify
     * our plugins that we're going down when some calls ctrl+c or just kills our process..
     */
    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stop()));
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

    private static void initDefaultWebEncoding() {
        String _defaultWebEncoding = configuration.getProperty("application.web_encoding");
        if (_defaultWebEncoding != null) {
            logger.info("Using custom default web encoding: {}", _defaultWebEncoding);
            defaultWebEncoding = _defaultWebEncoding;
            // Must update current response also, since the request/response triggering
            // this configuration-loading in dev-mode have already been
            // set up with the previous encoding
            if (Http.Response.current() != null) {
                Http.Response.current().encoding = _defaultWebEncoding;
            }
        }
    }

    public static synchronized void stop() {
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

    public static synchronized void detectChanges() {
        Router.detectChanges();
        pluginCollection.detectChange();
    }

    @SuppressWarnings("unchecked")
    public static <T extends PlayPlugin> T plugin(Class<T> clazz) {
        return pluginCollection.getPluginInstance(clazz);
    }

    private static void loadModules(VirtualFile appRoot) {
        File localModules = Play.getFile("modules");
        if (localModules.exists() && localModules.isDirectory()) {
            for (File module : localModules.listFiles()) {
                if (module == null || !module.exists()) {
                    logger.error("Module {} will not be loaded because {} does not exist", module.getName(), module.getAbsolutePath());
                } else if (module.isDirectory()) {
                    addModule(appRoot, module.getName(), module);
                } else {
                    File modulePath = new File(IO.readContentAsString(module).trim());
                    if (!modulePath.exists() || !modulePath.isDirectory()) {
                        logger.error("Module {} will not be loaded because {} does not exist", module.getName(), modulePath.getAbsolutePath());
                    } else {
                        addModule(appRoot, module.getName(), modulePath);
                    }
                }
            }
        }
    }

    /**
     * Add a play application (as plugin)
     *
     * @param appRoot
     *            the application path virtual file
     * @param name
     *            the module name
     * @param path
     *            The application path
     */
    private static void addModule(VirtualFile appRoot, String name, File path) {
        VirtualFile root = VirtualFile.open(path);
        modules.put(name, root);
        if (root.child("app").exists()) {
            javaPath.add(root.child("app"));
        }
        if (root.child("app/views").exists()
                || (usePrecompiled && appRoot.child("precompiled/templates/from_module_" + name + "/app/views").exists())) {
            templatesPath.add(root.child("app/views"));
        }
        if (root.child("conf/routes").exists()
                || (usePrecompiled && appRoot.child("precompiled/templates/from_module_" + name + "/conf/routes").exists())) {
            modulesRoutes.put(name, root.child("conf/routes"));
        }
        roots.add(root);
        if (!name.startsWith("_")) {
            logger.info("Module {} is available ({})", name, path.getAbsolutePath());
        }
    }

    /**
     * Search a VirtualFile in all loaded applications and plugins
     *
     * @param path
     *            Relative path from the applications root
     * @return The virtualFile or null
     */
    public static VirtualFile getVirtualFile(String path) {
        return VirtualFile.search(roots, path);
    }

    /**
     * Search a File in the current application
     *
     * @param path
     *            Relative path from the application root
     * @return The file even if it doesn't exist
     */
    public static File getFile(String path) {
        return new File(applicationPath, path);
    }

    /**
     * Returns true if application is runing in test-mode. Test-mode is resolved from the framework id.
     *
     * Your app is running in test-mode if the framwork id (Play.id) is 'test' or 'test-?.*'
     * 
     * @return true if testmode
     */
    public static boolean runningInTestMode() {
        return id.matches("test|test-?.*");
    }

    /**
     * Call this method when there has been a fatal error that Play cannot recover from
     */
    public static void fatalServerErrorOccurred() {
        // Just quit the process
        System.exit(-1);
    }

    public static boolean useDefaultMockMailSystem() {
        return "mock".equals(configuration.getProperty("mail.smtp", "")) && mode == Mode.DEV;
    }

}
