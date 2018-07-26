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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Collections.list;
import static java.util.Objects.hash;
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
    protected List<PlayPlugin> allPlugins = new ArrayList<>();

    /**
     * Readonly copy of allPlugins - updated each time allPlugins is updated. Using this cached copy so we don't have to
     * create it all the time..
     */
    protected List<PlayPlugin> allPlugins_readOnlyCopy = createReadonlyCopy(allPlugins);

    /**
     * List of all enabled plugins
     */
    protected List<PlayPlugin> enabledPlugins = new ArrayList<>();

    /**
     * Readonly copy of enabledPlugins - updated each time enabledPlugins is updated. Using this cached copy so we don't
     * have to create it all the time
     */
    protected List<PlayPlugin> enabledPlugins_readOnlyCopy = createReadonlyCopy(enabledPlugins);

    /**
     * Using readonly list to crash if someone tries to modify the copy.
     * 
     * @param list
     *            The list of plugins
     * @return Read only list of plugins
     */
    protected List<PlayPlugin> createReadonlyCopy(List<PlayPlugin> list) {
        return Collections.unmodifiableList(new ArrayList<>(list));
    }

    private static class LoadingPluginInfo implements Comparable<LoadingPluginInfo> {
        public final String name;
        public final int index;
        public final URL url;

        private LoadingPluginInfo(String name, int index, URL url) {
            this.name = name;
            this.index = index;
            this.url = url;
        }

        @Override
        public String toString() {
            return String.format("LoadingPluginInfo{name='%s', index=%s, url=%s}", name, index, url);
        }

        @Override
        public int compareTo(LoadingPluginInfo o) {
            int res = Integer.compare(index, o.index);
            if (res != 0) {
                return res;
            }

            // Index is equal in both plugins.
            // sort on name to get consistent order
            return name.compareTo(o.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            LoadingPluginInfo that = (LoadingPluginInfo) o;
            return Objects.equals(index, that.index) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return hash(name, index);
        }
    }

    public void loadPlugins() {
        logger.trace("Loading plugins");
        List<URL> urls = loadPlayPluginDescriptors();

        // First we build one big SortedSet of all plugins to load (sorted based on index)
        // This must be done to make sure the enhancing is happening
        // when loading plugins using other classes that must be enhanced.
        SortedSet<LoadingPluginInfo> pluginsToLoad = new TreeSet<>();
        for (URL url : urls) {
            logger.trace("Found one plugins descriptor, {}", url);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    String[] lineParts = line.split(":");
                    LoadingPluginInfo info = new LoadingPluginInfo(lineParts[1].trim(), Integer.parseInt(lineParts[0]), url);
                    pluginsToLoad.add(info);
                }
            } catch (Exception e) {
                logger.error("Error interpreting {}", url, e);
            }
        }

        for (LoadingPluginInfo info : pluginsToLoad) {
            logger.trace("Loading plugin {}", info.name);
            try {
                PlayPlugin plugin = (PlayPlugin) Injector.getBeanOfType(Class.forName(info.name));
                plugin.index = info.index;
                if (addPlugin(plugin)) {
                    logger.trace("Plugin {} loaded", plugin);
                } else {
                    logger.warn("Did not load plugin {}. Already loaded", plugin);
                }
            } catch (Exception ex) {
                logger.error("Error loading plugin {}", info, ex);
            }
        }
        // Now we must call onLoad for all plugins - and we must detect if a
        // plugin
        // disables another plugin the old way, by removing it from
        // Play.plugins.
        for (PlayPlugin plugin : getEnabledPlugins()) {

            // is this plugin still enabled?
            if (isEnabled(plugin)) {
                initializePlugin(plugin);
            }
        }
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
     * Calls plugin.onLoad but detects if plugin removes other plugins from Play.plugins-list to detect if plugins
     * disables a plugin the old hacked way..
     * 
     * @param plugin
     *            The given plugin
     */
    protected void initializePlugin(PlayPlugin plugin) {
        logger.trace("Initializing plugin {}", plugin);
        // We're ready to call onLoad for this plugin.
        // must create a unique Play.plugins-list for this onLoad-method-call so
        // we can detect if some plugins are removed/disabled
        List<PlayPlugin> plugins = new ArrayList<>(getEnabledPlugins());
        plugin.onLoad();
        // Check for missing/removed plugins
        for (PlayPlugin enabledPlugin : getEnabledPlugins()) {
            if (!plugins.contains(enabledPlugin)) {
                logger.info("Detected that plugin '{}' disabled the plugin '{}' the old way - should use Play.disablePlugin()", plugin, enabledPlugin);
                // This enabled plugin was disabled.
                // must disable it in pluginCollection
                disablePlugin(enabledPlugin);
            }
        }
    }

    /**
     * Adds one plugin and enables it
     * 
     * @param plugin
     *            The given plugin
     * @return true if plugin was new and was added
     */
    protected synchronized boolean addPlugin(PlayPlugin plugin) {
        if (!allPlugins.contains(plugin)) {
            allPlugins.add(plugin);
            Collections.sort(allPlugins);
            allPlugins_readOnlyCopy = createReadonlyCopy(allPlugins);
            enablePlugin(plugin);
            return true;
        }
        return false;
    }

    /**
     * Enable plugin.
     *
     * @param plugin
     *            The given plugin
     * @return true if plugin exists and was enabled now
     */
    public synchronized boolean enablePlugin(PlayPlugin plugin) {
        if (allPlugins.contains(plugin)) {
            // the plugin exists
            if (!enabledPlugins.contains(plugin)) {
                // plugin not currently enabled
                enabledPlugins.add(plugin);
                Collections.sort(enabledPlugins);
                enabledPlugins_readOnlyCopy = createReadonlyCopy(enabledPlugins);
                logger.trace("Plugin {} enabled", plugin);
                return true;
            }
        }

        return false;
    }

    /**
     * enable plugin of specified type
     * 
     * @param pluginClazz
     *            The plugin class
     * 
     * @return true if plugin was enabled
     */
    public boolean enablePlugin(Class<? extends PlayPlugin> pluginClazz) {
        return enablePlugin(getPluginInstance(pluginClazz));
    }

    /**
     * Returns the first instance of a loaded plugin of specified type
     * 
     * @param pluginClazz
     *            The plugin class
     * @return PlayPlugin
     */
    public synchronized <T extends PlayPlugin> T getPluginInstance(Class<T> pluginClazz) {
        for (PlayPlugin p : getAllPlugins()) {
            if (pluginClazz.isInstance(p)) {
                return (T) p;
            }
        }
        return null;
    }

    /**
     * disable plugin
     * 
     * @param plugin
     *            The given plugin
     * @return true if plugin was enabled and now is disabled
     */
    public synchronized boolean disablePlugin(PlayPlugin plugin) {
        if (enabledPlugins.remove(plugin)) {
            enabledPlugins_readOnlyCopy = createReadonlyCopy(enabledPlugins);
            logger.trace("Plugin {} disabled", plugin);
            return true;
        }
        return false;
    }

    /**
     * Disable plugin of specified type
     * 
     * @param pluginClazz
     *            The plugin class
     * 
     * @return true if plugin was enabled and now is disabled
     */
    public boolean disablePlugin(Class<? extends PlayPlugin> pluginClazz) {
        return disablePlugin(getPluginInstance(pluginClazz));
    }

    /**
     * Returns new readonly list of all enabled plugins
     * 
     * @return List of plugins
     */
    public List<PlayPlugin> getEnabledPlugins() {
        return enabledPlugins_readOnlyCopy;
    }

    /**
     * Returns readonly view of all enabled plugins in reversed order
     * 
     * @return Collection of plugins
     */
    List<PlayPlugin> getReversedEnabledPlugins() {
        ArrayList<PlayPlugin> reversedPlugins = new ArrayList<>(enabledPlugins);
        Collections.reverse(reversedPlugins);
        return reversedPlugins;
    }

    /**
     * Returns new readonly list of all plugins
     * 
     * @return List of plugins
     */
    public List<PlayPlugin> getAllPlugins() {
        return allPlugins_readOnlyCopy;
    }

    /**
     * Indicate if a plugin is enabled
     * 
     * @param plugin
     *            The given plugin
     * @return true if plugin is enabled
     */
    public boolean isEnabled(PlayPlugin plugin) {
        return getEnabledPlugins().contains(plugin);
    }

    public void onJobInvocationFinally() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onJobInvocationFinally();
        }
    }

    public void beforeInvocation() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.beforeInvocation();
        }
    }

    public void afterInvocation() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterInvocation();
        }
    }

    public void onInvocationSuccess() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onInvocationSuccess();
        }
    }

    public void onActionInvocationException(@Nonnull Http.Request request, @Nonnull Response response, @Nonnull Throwable e) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            try {
                plugin.onActionInvocationException(request, response, e);
            } catch (Throwable ex) {
                logger.error("Failed to handle action invocation exception by plugin {}", plugin.getClass().getName(), ex);
            }
        }
    }

    public void onJobInvocationException(@Nonnull Throwable e) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            try {
                plugin.onJobInvocationException(e);
            } catch (Throwable ex) {
                logger.error("Failed to handle job invocation exception by plugin {}", plugin.getClass().getName(), ex);
            }
        }
    }

    public void detectChange() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.detectChange();
        }
    }

    public void onConfigurationRead() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onConfigurationRead();
        }
    }

    public void onApplicationStart() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onApplicationStart();
        }
    }

    public void afterApplicationStart() {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterApplicationStart();
        }
    }

    public void onApplicationStop() {
        for (PlayPlugin plugin : getReversedEnabledPlugins()) {
            try {
                plugin.onApplicationStop();
            } catch (Throwable t) {
                logger.error("Error while stopping {}", plugin, t);
            }
        }
    }

    public void onEvent(String message, Object context) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onEvent(message, context);
        }
    }

    public Object bind(Http.Request request, Session session, RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Object result = plugin.bind(request, session, rootParamNode, name, clazz, type, annotations);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public String getMessage(String locale, Object key, Object... args) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            String message = plugin.getMessage(locale, key, args);
            if (message != null) {
                return message;
            }
        }
        return null;
    }

    public void beforeActionInvocation(Request request, Response response, Session session, RenderArgs renderArgs,
                                       Flash flash, Method actionMethod) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.beforeActionInvocation(request, response, session, renderArgs, flash, actionMethod);
        }
    }

    public void onActionInvocationResult(Request request, Response response, Session session, RenderArgs renderArgs,
                                         Result result) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onActionInvocationResult(request, response, session, renderArgs, result);
        }
    }

    public void afterActionInvocation(Request request, Response response, Flash flash) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.afterActionInvocation(request, response, flash);
        }
    }

    public void onActionInvocationFinally(@Nonnull Request request, @Nonnull Session session) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.onActionInvocationFinally(request, session);
        }
    }

    public void routeRequest(Request request) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            plugin.routeRequest(request);
        }
    }

    public boolean rawInvocation(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) throws Exception {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            if (plugin.rawInvocation(request, response, session, renderArgs, flash)) {
                return true;
            }
        }
        return false;
    }

    public Template loadTemplate(VirtualFile file) {
        for (PlayPlugin plugin : getEnabledPlugins()) {
            Template pluginProvided = plugin.loadTemplate(file);
            if (pluginProvided != null) {
                return pluginProvided;
            }
        }
        return null;
    }
}
