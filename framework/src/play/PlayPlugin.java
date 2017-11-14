package play;

import com.google.gson.JsonObject;
import play.data.binding.RootParamNode;
import play.db.Model;
import play.libs.F;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router.Route;
import play.mvc.results.Result;
import play.templates.Template;
import play.vfs.VirtualFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A framework plugin
 */
public abstract class PlayPlugin implements Comparable<PlayPlugin> {

    /**
     * Plugin priority (0 for highest priority)
     */
    public int index;

    /**
     * Called at plugin loading
     */
    public void onLoad() {
    }

    public boolean compileSources() {
        return false;
    }

    /**
     * Called when play need to bind a Java object from HTTP params.
     * 
     * @param rootParamNode
     *            parameters to bind
     * @param name
     *            the name of the object
     * @param clazz
     *            the class of the object to bind
     * @param type
     *            type
     * @param annotations
     *            annotation on the object
     * @return binding object
     */
    public Object bind(RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
        return null;
    }

    /**
     * Translate the given key for the given locale and arguments. If null is returned, Play's normal message
     * translation mechanism will be used.
     * 
     * @param locale
     *            the locale we want
     * @param key
     *            the message key
     * @param args
     *            arguments of the messages
     * @return the formatted string
     */
    public String getMessage(String locale, Object key, Object... args) {
        return null;
    }

    /**
     * Return the plugin status
     * 
     * @return the plugin status
     */
    public String getStatus() {
        return null;
    }

    /**
     * Return the plugin status in JSON format
     * 
     * @return the plugin status in JSON format
     */
    public JsonObject getJsonStatus() {
        return null;
    }

    /**
     * This hook is not plugged, don't implement it
     * 
     * @param template
     *            the template to compile
     * @deprecated
     */
    @Deprecated
    public void onTemplateCompilation(Template template) {
    }

    /**
     * Give a chance to this plugin to fully manage this request
     * 
     * @param request
     *            The Play request
     * @param response
     *            The Play response
     * @return true if this plugin has managed this request
     * @throws java.lang.Exception
     *             if cannot enhance the class
     */
    public boolean rawInvocation(Request request, Response response) throws Exception {
        return false;
    }

    /**
     * Let a chance to this plugin to manage a static resource
     * 
     * @param file
     *            The requested file
     * @param request
     *            The Play request
     * @param response
     *            The Play response
     * @return true if this plugin has managed this request
     */
    public boolean serveStatic(VirtualFile file, Request request, Response response) {
        return false;
    }

    /**
     * @param file
     *            the file of the template to load
     * @return the template object
     */
    public Template loadTemplate(VirtualFile file) {
        return null;
    }

    /**
     * It's time for the plugin to detect changes. Throw an exception is the application must be reloaded.
     */
    public void detectChange() {
    }

    /**
     * Called at application start (and at each reloading) Time to start stateful things.
     */
    public void onApplicationStart() {
    }

    /**
     * Called after the application start.
     */
    public void afterApplicationStart() {
    }

    /**
     * Called at application stop (and before each reloading) Time to shutdown stateful things.
     */
    public void onApplicationStop() {
    }

    /**
     * Called before a Play! invocation. Time to prepare request specific things.
     */
    public void beforeInvocation() {
    }

    /**
     * Called after an invocation. (unless an exception has been thrown). Time to close request specific things.
     */
    public void afterInvocation() {
    }

    /**
     * Called if an exception occurred during the invocation.
     * 
     * @param e
     *            The caught exception.
     */
    public void onInvocationException(Throwable e) {
    }

    /**
     * Called at the end of the invocation. (even if an exception occurred). Time to close request specific things.
     */
    public void invocationFinally() {
    }

    /**
     * Called before an 'action' invocation, ie an HTTP request processing.
     * 
     * @param actionMethod
     *            name of the method
     */
    public void beforeActionInvocation(Method actionMethod) {
    }

    /**
     * Called when the action method has thrown a result.
     * 
     * @param result
     *            The result object for the request.
     */
    public void onActionInvocationResult(Result result) {
    }

    public void onInvocationSuccess() {
    }

    /**
     * Called when the request has been routed.
     * 
     * @param route
     *            The route selected.
     */
    public void onRequestRouting(Route route) {
    }

    /**
     * Called at the end of the action invocation.
     */
    public void afterActionInvocation() {
    }

    /**
     * Called at the end of the action invocation (either in case of success or any failure).
     */
    public void onActionInvocationFinally() {
    }

    /**
     * Called when the application.conf has been read.
     */
    public void onConfigurationRead() {
    }

    /**
     * Called after routes loading.
     */
    public void onRoutesLoaded() {
    }

    /**
     * Event may be sent by plugins or other components
     * 
     * @param message
     *            convention: pluginClassShortName.message
     * @param context
     *            depends on the plugin
     */
    public void onEvent(String message, Object context) {
    }

    /**
     * @return List of the template extension
     */
    public List<String> addTemplateExtensions() {
        return emptyList();
    }

    /**
     * Let some plugins route themself
     * 
     * @param request
     *            the current request
     */
    public void routeRequest(Request request) {
    }

    /**
     * @param modelClass
     *            class of the model
     * @return the Model factory
     */
    public Model.Factory modelFactory(Class<? extends Model> modelClass) {
        return null;
    }

    /**
     * Inter-plugin communication.
     * 
     * @param message
     *            the message to post
     * @param context
     *            an object
     */
    public static void postEvent(String message, Object context) {
        Play.pluginCollection.onEvent(message, context);
    }

    public void onApplicationReady() {
    }

    @Override
    public int compareTo(PlayPlugin o) {
        int res = Integer.compare(index, o.index);
        if (res != 0) {
            return res;
        }

        // index is equal in both plugins.
        // Sort on class type to get consistent order
        res = this.getClass().getName().compareTo(o.getClass().getName());
        if (res != 0) {
            // class names where different
            return res;
        }

        // Identical classnames.
        // Sort on instance to get consistent order.
        // We only return 0 (equal) if both identityHashCode are identical
        // which is only the case if both this and other are the same object instance.
        // This is consistent with equals() when no special equals-method is implemented.
        int thisHashCode = System.identityHashCode(this);
        int otherHashCode = System.identityHashCode(o);
        return Integer.compare(thisHashCode, otherHashCode);
    }

    /**
     * Class that define a filter. A filter is a class that wrap a certain behavior around an action. You can access
     * your Request and Response object within the filter. See the JPA plugin for an example. The JPA plugin wraps a
     * transaction around an action. The filter applies a transaction to the current Action.
     */
    public abstract static class Filter<T> {
        String name;

        public Filter(String name) {
            this.name = name;
        }

        public abstract T withinFilter(play.libs.F.Function0<T> fct) throws Throwable;

        /**
         * Surround innerFilter with this. (innerFilter after this)
         * 
         * @param innerFilter
         *            filter to be wrapped.
         * @return a new Filter object. newFilter.withinFilter(x) is
         *         outerFilter.withinFilter(innerFilter.withinFilter(x))
         */
        public Filter<T> decorate(final Filter<T> innerFilter) {
            final Filter<T> outerFilter = this;
            return new Filter<T>(this.name) {
                @Override
                public T withinFilter(F.Function0<T> fct) throws Throwable {
                    return compose(outerFilter.asFunction(), innerFilter.asFunction()).apply(fct);
                }
            };
        }

        /**
         * Compose two second order functions whose input is a zero param function that returns type T...
         * 
         * @param outer
         *            Function that will wrap inner -- ("outer after inner")
         * @param inner
         *            Function to be wrapped by outer function -- ("outer after inner")
         * @return A function that computes outer(inner(x)) on application.
         */
        private static <T> Function1<F.Function0<T>, T> compose(final Function1<F.Function0<T>, T> outer,
                final Function1<F.Function0<T>, T> inner) {

            return new Function1<F.Function0<T>, T>() {
                @Override
                public T apply(final F.Function0<T> arg) throws Throwable {
                    return outer.apply(new F.Function0<T>() {
                        @Override
                        public T apply() throws Throwable {
                            return inner.apply(arg);
                        }
                    });
                }
            };
        }

        private final Function1<play.libs.F.Function0<T>, T> _asFunction = new Function1<F.Function0<T>, T>() {
            @Override
            public T apply(F.Function0<T> arg) throws Throwable {
                return withinFilter(arg);
            }
        };

        public Function1<play.libs.F.Function0<T>, T> asFunction() {
            return _asFunction;
        }

        public String getName() {
            return name;
        }

        // I don't want to add any additional dependencies to the project or use JDK 8 features
        // so I'm just rolling my own 1 arg function interface... there must be a better way to do this...
        public static interface Function1<I, O> {
            public O apply(I arg) throws Throwable;
        }
    }

    public final boolean hasFilter() {
        return this.getFilter() != null;
    }

    /**
     * Return the filter implementation for this plugin.
     * 
     * @return filter object of this plugin
     */
    public Filter getFilter() {
        return null;
    }

}
