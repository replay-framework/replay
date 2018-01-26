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
import play.mvc.Router.Route;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.*;
import play.utils.Java;
import play.utils.Utils;

import java.io.File;
import java.io.InputStream;
import java.lang.Error;
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
            Route route = Router.route(request);
            Play.pluginCollection.onRequestRouting(route);
        }
        request.resolveFormat();

        // Find the action method
        try {
            Method actionMethod;
            Object[] ca = getActionMethod(request.action);
            actionMethod = (Method) ca[1];
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

    private static void initActionContext(Http.Request request, Http.Response response, Session session, Flash flash) {
        Http.Request.setCurrent(request);
        Http.Response.setCurrent(response);

        Scope.Params.setCurrent(request.params);
        RenderArgs.current.set(new RenderArgs());
        Scope.RouteArgs.current.set(new Scope.RouteArgs());

        Session.current.set(session);
        Flash.current.set(flash);
        CachedBoundActionMethodArgs.init();
    }

    public static void invoke(Http.Request request, Http.Response response) {
        Monitor monitor = null;
        Session session = Session.restore(request);
        Flash flash = Flash.restore(request);
        initActionContext(request, response, session, flash);

        try {
            Method actionMethod = request.invokedMethod;

            Play.pluginCollection.beforeActionInvocation(request, response, session, RenderArgs.current(), actionMethod);

            // Monitoring
            monitor = MonitorFactory.start(request.action + "()");

            String cacheKey = null;
            Result actionResult = null;

            // 3. Invoke the action
            try {
                // @Before
                handleBefores(request);

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
                    inferResult(invokeControllerMethod(request, actionMethod));
                }
            } catch (Result result) {
                actionResult = result;
                // Cache it if needed
                if (cacheKey != null) {
                    Cache.set(cacheKey, actionResult, actionMethod.getAnnotation(CacheFor.class).value());
                }
            } catch (Exception e) {
                invokeControllerCatchMethods(request, e);
                throw e;
            }

            // @After
            handleAfters(request);

            monitor.stop();
            monitor = null;

            // OK, re-throw the original action result
            if (actionResult != null) {
                throw actionResult;
            }

            throw new NoResult();

        } catch (Result result) {
            Play.pluginCollection.onActionInvocationResult(result);

            // OK there is a result to apply
            // Save session & flash scope now
            session.save(request, response);
            flash.save(request, response);

            result.apply(request, response);

            Play.pluginCollection.afterActionInvocation(request, response, flash);

            // @Finally
            handleFinallies(request, null);

        } catch (RuntimeException e) {
            handleFinallies(request, e);
            throw e;
        } catch (Throwable e) {
            handleFinallies(request, e);
            throw new UnexpectedException(e);
        } finally {
            Play.pluginCollection.onActionInvocationFinally();

            if (monitor != null) {
                monitor.stop();
            }
        }
    }

    private static void invokeControllerCatchMethods(Http.Request request, Throwable throwable) throws Exception {
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
                    inferResult(invokeControllerMethod(request, mCatch, args));
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

    private static void handleBefores(Http.Request request) throws Exception {
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
                inferResult(invokeControllerMethod(request, before));
            }
        }
    }

    private static void handleAfters(Http.Request request) throws Exception {
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
                inferResult(invokeControllerMethod(request, after));
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
    static void handleFinallies(Http.Request request, Throwable caughtException) throws PlayException {

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
                        invokeControllerMethod(request, aFinally, new Object[] { caughtException });
                    } else {
                        // invoke @Finally-method the regular way without
                        // caughtException
                        invokeControllerMethod(request, aFinally, null);
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

    static Object invokeControllerMethod(Http.Request request, Method method) throws Exception {
        return invokeControllerMethod(request, method, null);
    }

    static Object invokeControllerMethod(Http.Request request, Method method, Object[] forceArgs) throws Exception {
        boolean isStatic = Modifier.isStatic(method.getModifiers());

        if (!isStatic && request.controllerInstance == null) {
            request.controllerInstance = Injector.getBeanOfType(request.controllerClass);
        }

        Object[] args = forceArgs != null ? forceArgs : getActionMethodArgs(request, method);

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

            throw new PlayException(originalThrowable);
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

    public static Object[] getActionMethodArgs(Http.Request request, Method method) throws Exception {
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
            rArgs[i] = Binder.bind(root, paramsNames[i], method.getParameterTypes()[i], method.getGenericParameterTypes()[i],
                    method.getParameterAnnotations()[i]);
        }

        CachedBoundActionMethodArgs.current().storeActionMethodArgs(method, rArgs);
        return rArgs;
    }
}
