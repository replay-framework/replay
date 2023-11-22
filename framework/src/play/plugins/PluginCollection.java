package play.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.ClasspathResource;
import play.Play;
import play.PlayPlugin;
import play.data.binding.RootParamNode;
import play.inject.Injector;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Result;
import play.templates.Template;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Class handling all plugins used by Play.
 *
 * Loading/reloading/enabling/disabling is handled here.
 *
 * This class also exposes many PlayPlugin-methods which when called, the method is executed on all enabled plugins.
 *
 * Since all the enabled-plugins-iteration is done here, the code elsewhere is cleaner.
 */
@ParametersAreNonnullByDefault
public class PluginCollection {
    private static final Logger logger = LoggerFactory.getLogger(PluginCollection.class);

    /**
     * List that holds all loaded plugins, both enabled and disabled
     */
    private final List<PlayPlugin> allPlugins = new ArrayList<>();

    public PluginCollection() {}

    public PluginCollection(SortedSet<PluginDescriptor> pluginsToLoad) {
        loadPlugins(pluginsToLoad);
    }

    /**
     * Load plugins from .plugin files, if any
     */
    public void loadPlugins() {
        logger.trace("Loading plugins");
        List<ClasspathResource> pluginsFiles = getPlayPluginFiles();

        // Sort plugins by index
        SortedSet<PluginDescriptor> pluginsToLoad = pluginsFiles.stream()
          .map(this::readPluginsDescriptor)
          .flatMap(List::stream)
          .collect(toCollection(TreeSet::new));

        loadPlugins(pluginsToLoad);
    }

    private List<PluginDescriptor> readPluginsDescriptor(ClasspathResource pluginsFile) {
        logger.trace("Found one plugins descriptor, {}", pluginsFile);
        return pluginsFile.lines().stream()
          .filter(line -> !line.trim().isEmpty())
          .map(line -> toPluginDescriptor(pluginsFile, line))
          .collect(toList());
    }

    @Nonnull
    @CheckReturnValue
    private PluginDescriptor toPluginDescriptor(ClasspathResource pluginsFile, String line) {
        String[] lineParts = line.split(":");
        return new PluginDescriptor(lineParts[1].trim(), parseInt(lineParts[0]), pluginsFile.url());
    }

    private void loadPlugins(SortedSet<PluginDescriptor> pluginsToLoad) {
        for (PluginDescriptor info : pluginsToLoad) {
            logger.trace("Loading plugin {}", info.name);
            PlayPlugin plugin = Injector.getBeanOfType(info.name);
            plugin.index = info.index;
            addPlugin(plugin);
            logger.trace("Plugin {} loaded", plugin);
        }
        // Now we call onLoad for all plugins, and we detect if a plugin disables another plugin the
        // old way, by removing it from Play.plugins.
        getEnabledPlugins().forEach(plugin -> {
            logger.trace("Initializing plugin {}", plugin);
            plugin.onLoad();
        });
    }

    List<ClasspathResource> getPlayPluginFiles() {
        String[] pluginsDescriptorFilenames = Play.configuration.getProperty("play.plugins.descriptor", "play.plugins").split(",");
        List<ClasspathResource> pluginDescriptors = Arrays.stream(pluginsDescriptorFilenames)
          .map(fileName -> ClasspathResource.files(fileName))
          .flatMap(List::stream)
          .collect(toList());
        logger.info("Found plugin descriptors: {}", pluginDescriptors);
        return pluginDescriptors;
    }

    /**
     * Adds one plugin and enables it
     * 
     * @param plugin
     *            The given plugin
     */
    protected synchronized void addPlugin(PlayPlugin plugin) {
        if (allPlugins.contains(plugin)) {
            throw new IllegalStateException("Plugin already added: " + plugin);
        }
        allPlugins.add(plugin);
        Collections.sort(allPlugins);
    }

    /**
     * Returns the first instance of a loaded plugin of specified type
     * 
     * @param pluginClazz
     *            The plugin class
     * @return PlayPlugin
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends PlayPlugin> T getPluginInstance(Class<T> pluginClazz) {
        return getAllPlugins()
          .filter(p -> pluginClazz.isInstance(p))
          .map(p -> (T) p)
          .findAny().orElse(null);
    }

    public Stream<PlayPlugin> getEnabledPlugins() {
        return getAllPlugins().filter(p -> p.isEnabled());
    }

    /**
     * Returns readonly view of all enabled plugins in reversed order
     * 
     * @return Collection of plugins
     */
    Stream<PlayPlugin> getReversedEnabledPlugins() {
        return getEnabledPlugins().sorted(Collections.reverseOrder());
    }

    /**
     * Returns new readonly list of all plugins
     * 
     * @return List of plugins
     */
    public Stream<PlayPlugin> getAllPlugins() {
        return allPlugins.stream();
    }

    public void onJobInvocationFinally() {
        getEnabledPlugins().forEach(plugin -> plugin.onJobInvocationFinally());
    }

    public void beforeInvocation() {
        getEnabledPlugins().forEach(plugin -> plugin.beforeInvocation());
    }

    public void afterInvocation() {
        getEnabledPlugins().forEach(plugin -> plugin.afterInvocation());
    }

    public void onInvocationSuccess() {
        getEnabledPlugins().forEach(plugin -> plugin.onInvocationSuccess());
    }

    public void onActionInvocationException(@Nonnull Http.Request request, @Nonnull Response response, @Nonnull Throwable e) {
        getEnabledPlugins().forEach(plugin -> {
            try {
                plugin.onActionInvocationException(request, response, e);
            } catch (Throwable ex) {
                logger.error("Failed to handle action invocation exception by plugin {}", plugin.getClass().getName(), ex);
            }
        });
    }

    public void onJobInvocationException(@Nonnull Throwable e) {
        getEnabledPlugins().forEach(plugin -> {
            try {
                plugin.onJobInvocationException(e);
            } catch (Throwable ex) {
                logger.error("Failed to handle job invocation exception by plugin {}", plugin.getClass().getName(), ex);
            }
        });
    }

    public void detectChange() {
        getEnabledPlugins().forEach(plugin -> plugin.detectChange());
    }

    public void onConfigurationRead() {
        getEnabledPlugins().forEach(plugin -> plugin.onConfigurationRead());
    }

    public void onApplicationStart() {
        getEnabledPlugins().forEach(plugin -> plugin.onApplicationStart());
    }

    public void afterApplicationStart() {
        getEnabledPlugins().forEach(plugin -> plugin.afterApplicationStart());
    }

    public void onApplicationStop() {
        getReversedEnabledPlugins().forEach(plugin -> {
            try {
                plugin.onApplicationStop();
            } catch (Throwable t) {
                logger.error("Error while stopping {}", plugin, t);
            }
        });
    }

    public Optional<Object> bind(Http.Request request, Session session, RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        return getEnabledPlugins()
          .map(plugin -> plugin.bind(request, session, rootParamNode, name, clazz, type, annotations))
          .filter(result -> result != null)
          .findFirst();
    }

    public Optional<String> getMessage(String locale, Object key, Object... args) {
        return getEnabledPlugins()
          .map(plugin -> plugin.getMessage(locale, key, args))
          .filter(message -> message.isPresent())
          .findFirst()
          .orElse(Optional.empty());
    }

    public void beforeActionInvocation(Request request, Response response, Session session, RenderArgs renderArgs,
                                       Flash flash, Method actionMethod) {
        getEnabledPlugins().forEach(plugin ->
          plugin.beforeActionInvocation(request, response, session, renderArgs, flash, actionMethod));
    }

    public void onActionInvocationResult(Request request, Response response, Session session, Flash flash, RenderArgs renderArgs,
                                         Result result) {
        getEnabledPlugins().forEach(plugin ->
          plugin.onActionInvocationResult(request, response, session, flash, renderArgs, result));
    }

    public void afterActionInvocation(Request request, Response response, Session session, Flash flash) {
        getEnabledPlugins().forEach(plugin ->
          plugin.afterActionInvocation(request, response, session, flash));
    }

    public void onActionInvocationFinally(@Nonnull Request request, @Nonnull Response response) {
        getEnabledPlugins().forEach(plugin -> plugin.onActionInvocationFinally(request, response));
    }

    public void routeRequest(Request request) {
        getEnabledPlugins().forEach(plugin -> plugin.routeRequest(request));
    }

    public boolean rawInvocation(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        return getEnabledPlugins().map(plugin -> rawInvoke(request, response, session, renderArgs, flash, plugin))
          .filter(wasInvoked -> wasInvoked)
          .findFirst()
          .orElse(false);
    }

    private boolean rawInvoke(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash, PlayPlugin plugin) {
        try {
            return plugin.rawInvocation(request, response, session, renderArgs, flash);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Template> loadTemplate(File file) {
        return getEnabledPlugins()
          .map(plugin -> plugin.loadTemplate(file))
          .filter(template -> template.isPresent())
          .findFirst()
          .orElse(Optional.empty());
    }
}
