package play;

import com.google.gson.JsonObject;
import play.data.binding.RootParamNode;
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
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * A framework plugin
 */
@ParametersAreNonnullByDefault
public abstract class PlayPlugin implements Comparable<PlayPlugin> {

    /**
     * Plugin priority (0 for highest priority)
     */
    public int index;

    private boolean enabled = true;

    protected void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Called at plugin loading
     */
    public void onLoad() {
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
    public Object bind(Http.Request request, Session session, RootParamNode rootParamNode, String name, Class<?> clazz, Type type, Annotation[] annotations) {
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
    public Optional<String> getMessage(String locale, Object key, Object... args) {
        return Optional.empty();
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
     * Give a chance to this plugin to fully manage this request
     * 
     * @param request
     *            The Play request
     * @param response
     *            The Play response
     * @return true if this plugin has managed this request
     */
    public boolean rawInvocation(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) throws Exception {
        return false;
    }

    /**
     * @param file
     *            the file of the template to load
     * @return the template object
     */
    public Optional<Template> loadTemplate(File file) {
        return Optional.empty();
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
     * Called at application stop (and before each reloading) Time to shut down stateful things.
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

    public void onActionInvocationException(@Nonnull Request request, @Nonnull Response response, @Nonnull Throwable e) {
    }

    public void onJobInvocationException(@Nonnull Throwable e) {
    }

    public void onJobInvocationFinally() {
    }

    /**
     * Called before an 'action' invocation, ie an HTTP request processing.
     */
    public void beforeActionInvocation(Request request, Response response, Session session, RenderArgs renderArgs,
                                       Flash flash, Method actionMethod) {
    }


    /**
     * Called when the action method has thrown a result.
     * 
     * @param result
     *            The result object for the request.
     */
    public void onActionInvocationResult(@Nonnull Request request, @Nonnull Response response,
                                         @Nonnull Session session, @Nonnull Flash flash,
                                         @Nonnull RenderArgs renderArgs, @Nonnull Result result) {
        onActionInvocationResult(request, response, session, renderArgs, result);
    }

    /**
     * @deprecated Use/override method with flash parameter
     */
    @Deprecated
    public void onActionInvocationResult(Request request, Response response, Session session, RenderArgs renderArgs, Result result) {
    }

    public void onInvocationSuccess() {
    }

    /**
     * Called at the end of the action invocation.
     */
    public void afterActionInvocation(Request request, Response response, Session session, Flash flash) {
    }

    /**
     * Called at the end of the action invocation (either in case of success or any failure).
     * Time to close request-specific things.
     */
    public void onActionInvocationFinally(@Nonnull Request request, @Nonnull Response response) {
    }

    /**
     * Called when the application.conf has been read.
     */
    public void onConfigurationRead() {
    }

    /**
     * Let some plugins route themselves
     *
     * @param request
     *            the current request
     */
    public void routeRequest(Request request) {
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
}
