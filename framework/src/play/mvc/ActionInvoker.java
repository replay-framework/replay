package play.mvc;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.cache.Cache;
import play.cache.CacheFor;
import play.data.binding.Binder;
import play.data.binding.CachedBoundActionMethodArgs;
import play.data.binding.ParamNode;
import play.data.binding.RootParamNode;
import play.exceptions.ActionNotFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.inject.Injector;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.NoResult;
import play.mvc.results.NotFound;
import play.mvc.results.RenderBinary;
import play.mvc.results.RenderHtml;
import play.mvc.results.Result;
import play.utils.Java;
import play.utils.Utils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Invoke an action after an HTTP request.
 */
public class ActionInvoker {
    private static final Logger logger = LoggerFactory.getLogger(ActionInvoker.class);

    private final FlashStore flashStore = new FlashStore();

    @SuppressWarnings("unchecked")
    public static void resolve(Http.Request request) {

        if (!Play.started) {
            return;
        }

        if (request.resolved) {
            return;
        }

        // Route and resolve format if not already done
        if (request.action == null) {
            Play.pluginCollection.routeRequest(request);
            Router.instance.route(request);
        }
        request.resolveFormat();

        // Find the action method
        try {
            Object[] ca = getActionMethod(request.action);
            Method actionMethod = (Method) ca[1];
            request.controller = ((Class) ca[0]).getName().substring(12).replace("$", "");
            request.controllerClass = ((Class) ca[0]);
            request.actionMethod = actionMethod.getName();
            request.action = request.controller + "." + request.actionMethod;
            request.invokedMethod = actionMethod;

            logger.trace("------- {}", actionMethod);

            request.resolved = true;

        } catch (ActionNotFoundException e) {
            logger.error(e.getMessage(), e);
            throw new NotFound(String.format("%s action not found", e.getAction()));
        }

    }

    private static void initActionContext(Http.Request request, Http.Response response, Session session, RenderArgs renderArgs, Flash flash) {
        Http.Request.setCurrent(request);
        Http.Response.setCurrent(response);

        Scope.Params.setCurrent(request.params);
        RenderArgs.current.set(renderArgs);
        Scope.RouteArgs.current.set(new Scope.RouteArgs());

        Session.current.set(session);
        Flash.current.set(flash);
        CachedBoundActionMethodArgs.init();
    }

    public void invoke(Http.Request request, Http.Response response) {
        Monitor monitor = null;
        Session session = Session.restore(request);
        Flash flash = flashStore.restore(request);
        RenderArgs renderArgs = new RenderArgs();
        initActionContext(request, response, session, renderArgs, flash);

        if (!Modifier.isStatic(request.invokedMethod.getModifiers())) {
            request.controllerInstance = Injector.getBeanOfType(request.controllerClass);
        }

        try {
            Method actionMethod = request.invokedMethod;

            Play.pluginCollection.beforeActionInvocation(request, response, session, renderArgs, flash, actionMethod);

            // Monitoring
            monitor = MonitorFactory.start(request.action + "()");

            String cacheKey = null;
            Result actionResult = null;

            // 3. Invoke the action
            try {
                // @Before
                handleBefores(request, session);

                // Action

                // Check the cache (only for GET or HEAD)
                if ((request.method.equals("GET") || request.method.equals("HEAD")) && actionMethod.isAnnotationPresent(CacheFor.class)) {
                    cacheKey = actionMethod.getAnnotation(CacheFor.class).id();
                    if ("".equals(cacheKey)) {
                        cacheKey = "urlcache:" + request.url + request.querystring;
                    }
                    actionResult = Cache.get(cacheKey);
                }

                if (actionResult == null) {
                    inferResult(invokeControllerMethod(request, session, actionMethod));
                }
            } catch (Result result) {
                actionResult = result;
                // Cache it if needed
                if (cacheKey != null) {
                    Cache.set(cacheKey, actionResult, actionMethod.getAnnotation(CacheFor.class).value());
                }
            } catch (Exception e) {
                invokeControllerCatchMethods(request, session, e);
                throw e;
            }

            // @After
            handleAfters(request, session);

            monitor.stop();
            monitor = null;

            // OK, re-throw the original action result
            if (actionResult != null) {
                throw actionResult;
            }

            throw new NoResult();

        } catch (Result result) {
            applyResult(request, response, session, flash, renderArgs, result);
        } catch (RuntimeException e) {
            handleFinallies(request, session, e);
            throw e;
        } catch (Throwable e) {
            handleFinallies(request, session, e);
            throw new UnexpectedException(e);
        } finally {
            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    private void applyResult(Http.Request request, Http.Response response, Session session, Flash flash, RenderArgs renderArgs, Result result) {
        Play.pluginCollection.onActionInvocationResult(request, response, session, flash, renderArgs, result);

        session.save(request, response);

        try {
            result.apply(request, response, session, renderArgs, flash);
        }
        catch (Result anotherResult) {
            if (result == anotherResult) {
                // avoid endless recursion
                throw new IllegalArgumentException("result is rethrown: " + anotherResult);
            }
            else {
                // There is a weird ExcelPlugin that throws RenderExcel from inside ViewResult.apply().
                // In this case, we need to call RenderExcel.apply()
                applyResult(request, response, session, flash, renderArgs, anotherResult);
            }
        }

        Play.pluginCollection.afterActionInvocation(request, response, flash);

        // It's important to send "flash" and "session" cookies to browser AFTER html is applied.
        // Because sometimes html does change flash.
        // For example, some html might execute %{flash.discard('info')}%`
        flashStore.save(flash, request, response);

        handleFinallies(request, session, null);
    }

    private static void invokeControllerCatchMethods(Http.Request request, Session session, Throwable throwable) throws Exception {
        // @Catch
        Object[] args = new Object[] {throwable};
        List<Method> catches = Java.findAllAnnotatedMethods(request.controllerClass, Catch.class);
        for (Method mCatch : catches) {
            Class[] exceptions = mCatch.getAnnotation(Catch.class).value();
            if (exceptions.length == 0) {
                exceptions = new Class[]{Exception.class};
            }
            for (Class exception : exceptions) {
                if (exception.isInstance(args[0])) {
                    mCatch.setAccessible(true);
                    inferResult(invokeControllerMethod(request, session, mCatch, args));
                    break;
                }
            }
        }
    }

    private static boolean isActionMethod(Method method) {
        return !method.isAnnotationPresent(Before.class) &&
                !method.isAnnotationPresent(After.class) &&
                !method.isAnnotationPresent(Finally.class) &&
                !method.isAnnotationPresent(Catch.class) &&
                !method.isAnnotationPresent(Util.class);
    }

    /**
     * Find the first public method of a controller class
     *
     * @param name
     *            The method name
     * @param clazz
     *            The class
     * @return The method or null
     */
    public static Method findActionMethod(String name, Class clazz) {
        while (!"java.lang.Object".equals(clazz.getName())) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equalsIgnoreCase(name) && Modifier.isPublic(m.getModifiers())) {
                    // Check that it is not an interceptor
                    if (isActionMethod(m)) {
                        return m;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private static void handleBefores(Http.Request request, Session session) throws Exception {
        List<Method> befores = Java.findAllAnnotatedMethods(request.controllerClass, Before.class);
        for (Method before : befores) {
            String[] unless = before.getAnnotation(Before.class).unless();
            String[] only = before.getAnnotation(Before.class).only();
            boolean skip = false;
            for (String un : only) {
                if (!un.contains(".")) {
                    un = before.getDeclaringClass().getName().substring(12).replace("$", "") + "." + un;
                }
                if (un.equals(request.action)) {
                    skip = false;
                    break;
                } else {
                    skip = true;
                }
            }
            for (String un : unless) {
                if (!un.contains(".")) {
                    un = before.getDeclaringClass().getName().substring(12).replace("$", "") + "." + un;
                }
                if (un.equals(request.action)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                before.setAccessible(true);
                inferResult(invokeControllerMethod(request, session, before));
            }
        }
    }

    private static void handleAfters(Http.Request request, Session session) throws Exception {
        List<Method> afters = Java.findAllAnnotatedMethods(request.controllerClass, After.class);
        for (Method after : afters) {
            String[] unless = after.getAnnotation(After.class).unless();
            String[] only = after.getAnnotation(After.class).only();
            boolean skip = false;
            for (String un : only) {
                if (!un.contains(".")) {
                    un = after.getDeclaringClass().getName().substring(12) + "." + un;
                }
                if (un.equals(request.action)) {
                    skip = false;
                    break;
                } else {
                    skip = true;
                }
            }
            for (String un : unless) {
                if (!un.contains(".")) {
                    un = after.getDeclaringClass().getName().substring(12) + "." + un;
                }
                if (un.equals(request.action)) {
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                after.setAccessible(true);
                inferResult(invokeControllerMethod(request, session, after));
            }
        }
    }

    /**
     * Checks and calla all methods in controller annotated with @Finally. The
     * caughtException-value is sent as argument to @Finally-method if method
     * has one argument which is Throwable
     *
     * @param caughtException
     *            If @Finally-methods are called after an error, this variable
     *            holds the caught error
     */
    static void handleFinallies(Http.Request request, Session session, Throwable caughtException) throws PlayException {

        if (request.controllerClass == null) {
            // skip it
            return;
        }

        try {
            List<Method> allFinally = Java.findAllAnnotatedMethods(request.controllerClass, Finally.class);
            for (Method aFinally : allFinally) {
                String[] unless = aFinally.getAnnotation(Finally.class).unless();
                String[] only = aFinally.getAnnotation(Finally.class).only();
                boolean skip = false;
                for (String un : only) {
                    if (!un.contains(".")) {
                        un = aFinally.getDeclaringClass().getName().substring(12) + "." + un;
                    }
                    if (un.equals(request.action)) {
                        skip = false;
                        break;
                    } else {
                        skip = true;
                    }
                }
                for (String un : unless) {
                    if (!un.contains(".")) {
                        un = aFinally.getDeclaringClass().getName().substring(12) + "." + un;
                    }
                    if (un.equals(request.action)) {
                        skip = true;
                        break;
                    }
                }
                if (!skip) {
                    aFinally.setAccessible(true);

                    // check if method accepts Throwable as only parameter
                    Class[] parameterTypes = aFinally.getParameterTypes();
                    if (parameterTypes.length == 1 && parameterTypes[0] == Throwable.class) {
                        // invoking @Finally method with caughtException as
                        // parameter
                        invokeControllerMethod(request, session, aFinally, new Object[] { caughtException });
                    } else {
                        // invoke @Finally-method the regular way without
                        // caughtException
                        invokeControllerMethod(request, session, aFinally, null);
                    }
                }
            }
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new UnexpectedException("Exception while doing @Finally", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void inferResult(Object o) {
        // Return type inference
        if (o != null) {

            if (o instanceof NoResult) {
                return;
            }
            if (o instanceof Result) {
                // Of course
                throw (Result) o;
            }
            if (o instanceof InputStream) {
                throw new RenderBinary((InputStream) o, null, true);
            }
            if (o instanceof File) {
                throw new RenderBinary((File) o);
            }
            if (o instanceof Map) {
                throw new UnsupportedOperationException("Controller action cannot return Map");
            }
            throw new RenderHtml(o.toString());
        }
    }

    static Object invokeControllerMethod(Http.Request request, Session session, Method method) throws Exception {
        return invokeControllerMethod(request, session, method, null);
    }

    static Object invokeControllerMethod(Http.Request request, Session session, Method method, Object[] forceArgs) throws Exception {
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        Object[] args = forceArgs != null ? forceArgs : getActionMethodArgs(request, session, method);

        Object methodClassInstance = isStatic ? null :
           (method.getDeclaringClass().isAssignableFrom(request.controllerClass)) ? request.controllerInstance :
                Injector.getBeanOfType(method.getDeclaringClass());

        return invoke(method, methodClassInstance, args);
    }

    static Object invoke(Method method, Object instance, Object ... realArgs) throws Exception {
        try {
            return method.invoke(instance, realArgs);
        } catch (InvocationTargetException ex) {
            Throwable originalThrowable = ex.getTargetException();

            if (originalThrowable instanceof RuntimeException)
                throw (RuntimeException) originalThrowable;
            if (originalThrowable instanceof Exception)
                throw (Exception) originalThrowable;
            if (originalThrowable instanceof Error)
                throw (Error) originalThrowable;

            throw new RuntimeException(originalThrowable);
        }
    }

    public static Object[] getActionMethod(String fullAction) {
        Method actionMethod;
        Class controllerClass;
        try {
            if (!fullAction.startsWith("controllers.")) {
                fullAction = "controllers." + fullAction;
            }
            String controller = fullAction.substring(0, fullAction.lastIndexOf("."));
            String action = fullAction.substring(fullAction.lastIndexOf(".") + 1);
            controllerClass = Play.classes.getClassIgnoreCase(controller);
            if (controllerClass == null) {
                throw new ActionNotFoundException(fullAction, new Exception("Controller " + controller + " not found"));
            }
            actionMethod = findActionMethod(action, controllerClass);
            if (actionMethod == null) {
                throw new ActionNotFoundException(fullAction,
                        new Exception("No method public static void " + action + "() was found in class " + controller));
            }
        } catch (PlayException e) {
            throw e;
        } catch (Exception e) {
            throw new ActionNotFoundException(fullAction, e);
        }
        return new Object[] { controllerClass, actionMethod };
    }

    public static Object[] getActionMethodArgs(Http.Request request, Session session, Method method) {
        String[] paramsNames = Java.parameterNames(method);
        if (paramsNames == null && method.getParameterTypes().length > 0) {
            throw new UnexpectedException("Parameter names not found for method " + method);
        }

        // Check if we have already performed the bind operation
        Object[] rArgs = CachedBoundActionMethodArgs.current().retrieveActionMethodArgs(method);
        if (rArgs != null) {
            // We have already performed the binding-operation for this method
            // in this request.
            return rArgs;
        }

        rArgs = new Object[method.getParameterTypes().length];
        for (int i = 0; i < method.getParameterTypes().length; i++) {

            Class<?> type = method.getParameterTypes()[i];
            Map<String, String[]> params = new HashMap<>();

            // In case of simple params, we don't want to parse the body.
            if (type.equals(String.class) || Number.class.isAssignableFrom(type) || type.isPrimitive()) {
                params.put(paramsNames[i], request.params.getAll(paramsNames[i]));
            } else {
                params.putAll(request.params.all());
            }
            if (logger.isTraceEnabled()) {
                logger.trace("getActionMethodArgs name [{}] annotation [{}]", paramsNames[i], Utils.join(method.getParameterAnnotations()[i], " "));
            }

            RootParamNode root = ParamNode.convert(params);
            rArgs[i] = Binder.bind(request, session, root, paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i],
                    method.getParameterAnnotations()[i]);
        }

        CachedBoundActionMethodArgs.current().storeActionMethodArgs(method, rArgs);
        return rArgs;
    }
}
