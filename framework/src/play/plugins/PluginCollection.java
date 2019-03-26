package play.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import play.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.list;
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
public class PluginCollection {
    private static final Logger logger = LoggerFactory.getLogger(PluginCollection.class);

    /**
     * List that holds all loaded plugins, enabled or disabled
     */
    private final List<PlayPlugin> allPlugins = new ArrayList<>();

    public void loadPlugins() {
        logger.trace("Loading plugins");
        List<URL> urls = loadPlayPluginDescriptors();

        // First we build one big SortedSet of all plugins to load (sorted based on index)
        // This must be done to make sure the enhancing is happening
        // when loading plugins using other classes that must be enhanced.
        SortedSet<PluginDescriptor> pluginsToLoad = new TreeSet<>();
        for (URL url : urls) {
            logger.trace("Found one plugins descriptor, {}", url);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] lineParts = line.split(":");
                    PluginDescriptor info = new PluginDescriptor(lineParts[1].trim(), Integer.parseInt(lineParts[0]), url);
                    pluginsToLoad.add(info);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read plugins descriptor " + url, e);
            }
        }

        for (PluginDescriptor info : pluginsToLoad) {
            logger.trace("Loading plugin {}", info.name);
            PlayPlugin plugin = Injector.getBeanOfType(info.name);
            plugin.index = info.index;
            addPlugin(plugin);
            logger.trace("Plugin {} loaded", plugin);
        }
        // Now we must call onLoad for all plugins - and we must detect if a
        // plugin
        // disables another plugin the old way, by removing it from
        // Play.plugins.
        getEnabledPlugins().forEach(plugin -> {
          logger.trace("Initializing plugin {}", plugin);
          plugin.onLoad();
        });
    }

    List<URL> loadPlayPluginDescriptors() {
        String[] pluginsDescriptorFilenames = Play.configuration.getProperty("play.plugins.descriptor", "play.plugins").split(",");
        List<URL> pluginDescriptors = Arrays.stream(pluginsDescriptorFilenames)
          .map(f -> getResources(f))
          .flatMap(List::stream)
          .collect(toList());
        logger.info("Found plugin descriptors: {}", pluginDescriptors);
        return pluginDescriptors;
    }

    private List<URL> getResources(String f) {
        try {
            return list(Thread.currentThread().getContextClassLoader().getResources(f));
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Failed to read plugins from " + f);
        }
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

    public void afterActionInvocation(Request request, Response response, Flash flash) {
        getEnabledPlugins().forEach(plugin ->
          plugin.afterActionInvocation(request, response, flash));
    }

    public void onActionInvocationFinally(@Nonnull Request request) {
        getEnabledPlugins().forEach(plugin -> plugin.onActionInvocationFinally(request));
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

    public Optional<Template> loadTemplate(VirtualFile file) {
        return getEnabledPlugins()
          .map(plugin -> plugin.loadTemplate(file))
          .filter(template -> template.isPresent())
          .findFirst()
          .orElse(Optional.empty());
    }
}
