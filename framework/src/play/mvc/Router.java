package play.mvc;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.ClasspathResource;
import play.Play;
import play.Play.Mode;
import play.exceptions.NoRouteFoundException;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router.Route.Arg;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.mvc.routing.RoutesParser;
import play.utils.Default;
import play.utils.Utils;
import play.vfs.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.regex.Pattern.CASE_INSENSITIVE;

/**
 * The router matches HTTP requests to action invocations
 */
public class Router {
    private static final Logger logger = LoggerFactory.getLogger(Router.class);

    public static final Router instance = new Router(new CopyOnWriteArrayList<>());
    private static final Pattern HTTP_PROTO_REGEX = Pattern.compile("https?");

    /**
     * All the loaded routes.
     */
    private final List<Route> routes;

    private final Map<String, Map<String, Route>> parameterlessRoutes = new HashMap<>();

    /**
     * Timestamp the routes file was last loaded at.
     */
    public static long lastLoading = -1;

    public Router(List<Route> routes) {
        this.routes = routes;
    }

    public static List<Route> routes() {
        return instance.routes;
    }

    /**
     * Parse the routes file into the routing definitions. Install those definitions in the Router.
     *
     * This is called at normal startup.
     */
    private static void loadRoutesFromFile() {
        instance.setRoutes(new RoutesParser().parse(Play.routes));
        lastLoading = System.currentTimeMillis();
    }

    /**
     * Replace routes with provided route definitions.
     */
    public void setRoutes(List<Route> routes) {
        actionRoutesCache.clear();
        this.routes.clear();
        parameterlessRoutes.clear();
        routes.forEach(this::addRoute);
    }

    private void addRoute(Route route) {
        routes.add(route);

        if (route.pattern == null) {
            parameterlessRoutes
              .computeIfAbsent(route.method.toUpperCase(), (method) -> new HashMap<>())
              .put(route.path.toLowerCase(), route);
        }
    }

    private Route findParameterlessRoute(String method, String path) {
        return parameterlessRoutes.getOrDefault(method.toUpperCase(), emptyMap()).get(path.toLowerCase());
    }

    public static void clearForTests() {
        instance.setRoutes(emptyList());
    }

    /**
     * Add a new route. Will be first in the route list
     * 
     * @param method
     *            The method of the route * @param action : The associated action
     * @param path
     *            The path of the route
     * @param action
     *            The associated action
     */
    public static void addRoute(String method, String path, String action) {
        instance.addRoute(new Route(method, path, action, null, 0));
    }

    /**
     * <p>
     * In PROD mode and if the routes are already loaded, this does nothing.
     * </p>
     * <p>
     * In DEV mode, this checks each routes file's "last modified" time to see if the routes need updated.
     * </p>
     */
    public static void detectChanges() {
        if (Play.mode == Mode.PROD && lastLoading > 0) {
            return;
        }
        if (Play.routes == null) {
            return;
        }
        if (Play.routes.isModifiedAfter(lastLoading)) {
            loadRoutesFromFile();
        }
    }

    /**
     * @throws RenderStatic or NotFound
     */
    public void routeOnlyStatic(Request request) {
        Route parameterlessRoute = findParameterlessRoute(request.method, request.path);
        if (parameterlessRoute != null) {
            if (parameterlessRoute.matches(request.method, request.path) != null) {
                return;
            }
        }

        for (Route route : routes) {
            if (route.matches(request.method, request.path) != null) {
                return;
            }
        }
    }

    Route route(Request request) {
        logger.trace("Route: {} - {}", request.path, request.querystring);

        MatchingRoute match = matchRoute(request.method, request.path);
        if (match != null) return processRoute(request, match);

        // Not found - if the request was a HEAD, let's see if we can find a corresponding GET
        if ("HEAD".equalsIgnoreCase(request.method)) {
            request.method = "GET";
            Route route = route(request);
            request.method = "HEAD";
            if (route != null) return route;
        }
        throw new NotFound(request.method, request.path);
    }

    @Nullable public MatchingRoute matchRoute(String method, String path) {
        Route parameterlessRoute = findParameterlessRoute(method, path);
        if (parameterlessRoute != null) return new MatchingRoute(parameterlessRoute, emptyMap());

        for (Route route : routes) {
            Map<String, String> args = route.matches(method, path);
            if (args != null) return new MatchingRoute(route, args);
        }

        return null;
    }

    private Route processRoute(Request request, MatchingRoute match) {
        request.routeArgs = match.args;
        request.action = match.route.action;
        if (match.args.containsKey("format")) {
            request.format = match.args.get("format");
        }
        if (request.action.contains("{")) { // more optimization?
            for (Entry<String, String> arg : request.routeArgs.entrySet()) {
                request.action = request.action.replace("{" + arg.getKey() + "}", arg.getValue());
            }
        }
        if ("404".equals(request.action)) {
            throw new NotFound(match.route.path);
        }
        return match.route;
    }

    @Deprecated
    public static ActionDefinition reverse(String action) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it
        // will be copied and changed.
        return reverse(action, new HashMap<>(16));
    }

    @Deprecated
    @Nonnull
    public static String getFullUrl(String action, Map<String, Object> args) {
        return getFullUrl(action, args, Request.current());
    }

    @Nonnull
    public static String getFullUrl(String action, Map<String, Object> args, @Nullable Request request) {
        String baseUrl = getBaseUrl(request);
        return getFullUrl(action, args, baseUrl);
    }

    @Nonnull
    public static String getFullUrl(@Nonnull String action, @Nonnull Map<String, Object> args, @Nonnull String baseUrl) {
        ActionDefinition actionDefinition = reverse(action, args);
        if ("WS".equals(actionDefinition.method)) {
            return HTTP_PROTO_REGEX.matcher(baseUrl).replaceFirst("ws") + actionDefinition;
        }
        return baseUrl + actionDefinition;
    }

    /**
     * Gets baseUrl from current request or application.baseUrl in application.conf
     */
    @Deprecated
    @Nonnull
    public static String getBaseUrl() {
        return getBaseUrl(Request.current());
    }

    @Nonnull
    public static String getBaseUrl(@Nullable Request request) {
        if (request == null) {
            return getConfiguredBaseUrl();
        } else {
            return request.getBase();
        }
    }

    @Nonnull
    public static String getConfiguredBaseUrl() {
        String appBaseUrl = Play.configuration.getProperty("application.baseUrl", "application.baseUrl");
        if (appBaseUrl.endsWith("/")) {
            // remove the trailing slash
            appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length() - 1);
        }
        return appBaseUrl;
    }

    @Deprecated
    @Nonnull
    public static String getFullUrl(@Nonnull String action) {
        return getFullUrl(action, Request.current());
    }

    @Nonnull
    public static String getFullUrl(@Nonnull String action, @Nullable Request request) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it
        // will be copied and changed.
        return getFullUrl(action, new HashMap<>(16), request);
    }

    @Nonnull
    public static String getFullUrl(@Nonnull String action, @Nonnull String baseUrl) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it
        // will be copied and changed.
        return getFullUrl(action, new HashMap<>(16), baseUrl);
    }

    @Deprecated
    public static String reverse(VirtualFile file) {
        return instance.reverse(file, false);
    }

    private String reverse(VirtualFile file, boolean absolute) {
        if (file == null || !file.exists()) {
            throw new NoRouteFoundException("File not found (" + file + ")");
        }
        String path = file.relativePath();
        path = path.substring(path.indexOf('}') + 1);
        for (Route route : routes) {
            String staticDir = route.staticDir;
            if (staticDir != null) {
                if (!staticDir.startsWith("/")) {
                    staticDir = "/" + staticDir;
                }
                if (!staticDir.equals("/") && !staticDir.endsWith("/")) {
                    staticDir = staticDir + "/";
                }
                if (path.startsWith(staticDir)) {
                    String to = route.path + path.substring(staticDir.length());
                    if (to.endsWith("/index.html")) {
                        to = to.substring(0, to.length() - "/index.html".length() + 1);
                    }
                    if (absolute) {
                        to = getBaseUrl() + to;
                    }
                    return to;
                }
            }
        }
        throw new NoRouteFoundException(file.relativePath());
    }

    public static String reverseWithCheck(String name, VirtualFile file, boolean absolute) {
        if (file == null || !file.exists()) {
            throw new NoRouteFoundException(name + " (file not found)");
        }
        return instance.reverse(file, absolute);
    }

    @Deprecated
    @Nonnull
    public static ActionDefinition reverse(@Nonnull String action, @Nullable Map<String, Object> args) {
        return reverse(action, args, Request.current(), Response.current());
    }

    @Nonnull
    public static ActionDefinition reverse(@Nonnull String action, @Nullable Map<String, Object> args, @Nullable Request request, @Nullable Response response) {
        return instance.actionToUrl(action, args, request, response);
    }

    @Nonnull
    public static ActionDefinition reverse(@Nonnull String action, @Nullable Map<String, Object> args, @Nonnull String requestFormat, @Nullable Charset encoding) {
        return instance.actionToUrl(action, args, requestFormat, encoding);
    }

    @Nonnull
    public ActionDefinition actionToUrl(@Nonnull String action, @Nullable Map<String, Object> actionArgs, @Nullable Request request, @Nullable Response response) {
        var requestFormat = request == null || request.format == null ? "" : request.format;
        var responseEncoding = response == null ? null : response.encoding;
        return actionToUrl(action, actionArgs, requestFormat, responseEncoding);
    }

    @Nonnull
    public ActionDefinition actionToUrl(@Nonnull String action, @Nullable Map<String, Object> actionArgs, @Nonnull String requestFormat, @Nullable Charset encoding) {
        Map<String, Object> args = new LinkedHashMap<>(actionArgs);
        Charset actualEncoding = encoding == null ? Play.defaultWebEncoding : encoding;

        if (action.startsWith("controllers.")) {
            action = action.substring(12);
        }
        Map<String, Object> argsBackup = new HashMap<>(args);

        List<MatchingRoute> matchingRoutes = getActionRoutes(action);
        for (MatchingRoute actionRoute : matchingRoutes) {
            Route route = actionRoute.route;
            args.putAll(actionRoute.args);

            List<String> inPathArgs = new ArrayList<>(16);
            boolean allRequiredArgsAreHere = true;
            // do the parameter names match?
            for (Arg arg : route.args) {
                inPathArgs.add(arg.name);
                Object value = args.get(arg.name);
                if (value == null) {
                    allRequiredArgsAreHere = false;
                } else {
                    if (value instanceof List<?>) {
                        value = ((List<Object>) value).get(0);
                    }
                    if (!value.toString().startsWith(":") && !arg.constraint.matcher(Utils.urlEncodePath(value.toString())).matches()) {
                        allRequiredArgsAreHere = false;
                        break;
                    }
                }
            }
            // do the hardcoded parameters in the route match?
            for (String staticKey : route.staticArgs.keySet()) {
                if (staticKey.equals("format")) {
                    if (!requestFormat.equals(route.staticArgs.get("format"))) {
                        allRequiredArgsAreHere = false;
                        break;
                    }
                    continue; // format is a special key
                }
                if (!args.containsKey(staticKey) || (args.get(staticKey) == null)
                        || !args.get(staticKey).toString().equals(route.staticArgs.get(staticKey))) {
                    allRequiredArgsAreHere = false;
                    break;
                }
            }
            if (allRequiredArgsAreHere) {
                StringBuilder queryString = new StringBuilder();
                String path = route.path;
                String host = "";
                if (path.endsWith("/?")) {
                    path = path.substring(0, path.length() - 2);
                }
                for (Entry<String, Object> entry : args.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (inPathArgs.contains(key) && value != null) {
                        if (List.class.isAssignableFrom(value.getClass())) {
                            @SuppressWarnings("unchecked")
                            List<Object> values = (List<Object>) value;
                            path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", values.get(0).toString()).replace("$", "\\$");
                        } else {
                            path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString(), actualEncoding)
                                    .replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                            host = host.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString(), actualEncoding)
                                    .replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                        }
                    } else if (route.staticArgs.containsKey(key)) {
                        // Do nothing -> The key is static
                    } else if (!argsBackup.containsKey(key)) {
                        // Do nothing -> The key is provided in
                        // RouteArgs and not used (see #447)
                    } else if (value != null) {
                        if (List.class.isAssignableFrom(value.getClass())) {
                            @SuppressWarnings("unchecked")
                            List<Object> values = (List<Object>) value;
                            for (Object object : values) {
                                queryString.append(URLEncoder.encode(key, actualEncoding));
                                queryString.append("=");
                                String objStr = object.toString();
                                // Special case to handle jsAction
                                // tag
                                if (objStr.startsWith(":") && objStr.length() > 1) {
                                    queryString.append(':');
                                    objStr = objStr.substring(1);
                                }
                                queryString.append(URLEncoder.encode(objStr + "", actualEncoding));
                                queryString.append("&");
                            }
                        } else if (value.getClass().equals(Default.class)) {
                            // Skip defaults in queryString
                        } else {
                            queryString.append(URLEncoder.encode(key, actualEncoding));
                            queryString.append("=");
                            String objStr = value.toString();
                            // Special case to handle jsAction tag
                            if (objStr.startsWith(":") && objStr.length() > 1) {
                                queryString.append(':');
                                objStr = objStr.substring(1);
                            }
                            queryString.append(URLEncoder.encode(objStr + "", actualEncoding));
                            queryString.append("&");
                        }
                    }
                }
                String qs = queryString.toString();
                if (qs.endsWith("&")) {
                    qs = qs.substring(0, qs.length() - 1);
                }
                ActionDefinition actionDefinition = new ActionDefinition();
                actionDefinition.url = qs.isEmpty() ? path : path + "?" + qs;
                actionDefinition.method = route.method == null || "*".equals(route.method) ? "GET" : route.method.toUpperCase();
                actionDefinition.star = "*".equals(route.method);
                actionDefinition.action = action;
                actionDefinition.args = argsBackup;
                actionDefinition.host = host;
                return actionDefinition;
            }
        }

        throw new NoRouteFoundException(action, args);
    }

    private final Map<String, List<MatchingRoute>> actionRoutesCache = new ConcurrentHashMap<>();

    private List<MatchingRoute> getActionRoutes(String action) {
        return actionRoutesCache.computeIfAbsent(action, this::findActionRoutes);
    }

    private List<MatchingRoute> findActionRoutes(String action) {
        List<MatchingRoute> matchingRoutes = new ArrayList<>();
        for (Route route : routes) {
            if (route.actionPattern != null) {
                Matcher matcher = route.actionPattern.matcher(action);
                if (matcher.matches()) {
                    MatchingRoute matchingRoute = new MatchingRoute(route, new HashMap<>(route.actionArgs.size()));

                    for (String group : route.actionArgs) {
                        String v = matcher.group(group);
                        if (v == null) {
                            continue;
                        }
                        matchingRoute.args.put(group, v.toLowerCase());
                    }
                    matchingRoutes.add(matchingRoute);
                }
            }
        }
        return matchingRoutes;
    }

    public static class ActionDefinition {
        private static final Pattern HOST_REGEX =
            Pattern.compile("([-_a-z0-9A-Z]+([.][-_a-z0-9A-Z]+)?)$");

        /**
         * The domain/host name.
         */
        public String host;
        /**
         * The HTTP method, e.g. "GET".
         */
        public String method;
        /**
         * FIXME - what is this? does it include the domain?
         */
        public String url;
        /**
         * Whether the route contains an asterisks *.
         */
        public boolean star;
        /**
         * FIXME - what is this? does it include the class and package?
         */
        public String action;
        /**
         * FIXME - are these the required args in the routing file, or the query string in a request?
         */
        public Map<String, Object> args;

        public ActionDefinition add(String key, Object value) {
            args.put(key, value);
            return reverse(action, args);
        }

        public ActionDefinition remove(String key) {
            args.remove(key);
            return reverse(action, args);
        }

        @Override
        public String toString() {
            return url;
        }

        public void absolute(@Nullable Request request) {
            boolean isSecure = request != null && request.isSecure();
            String base = request == null ? getBaseUrl() : getBaseUrl(request);
            String hostPart = host;
            String domain = request == null ? "" : request.domain;
            int port = request == null ? 80 : request.port;
            if (port != 80 && port != 443) {
                hostPart += ":" + port;
            }

            if (!url.startsWith("http")) {
                if (StringUtils.isEmpty(host)) {
                    url = base + url;
                } else if (host.contains("{_}")) {
                    Matcher matcher = HOST_REGEX.matcher(domain);
                    if (matcher.find()) {
                        url = (isSecure ? "https://" : "http://") + hostPart.replace("{_}", matcher.group(1)) + url;
                    } else {
                        url = (isSecure ? "https://" : "http://") + hostPart + url;
                    }
                } else {
                    url = (isSecure ? "https://" : "http://") + hostPart + url;
                }
                if ("WS".equals(method)) {
                    url = HTTP_PROTO_REGEX.matcher(url).replaceFirst("ws");
                }
            }
        }
    }

    public static class Route {
        public final String method;
        public final String path;
        public final String action;
        final Pattern actionPattern;
        final List<String> actionArgs = new ArrayList<>(3);
        final String staticDir;
        final boolean staticFile;
        final Pattern pattern;
        final List<Arg> args = new ArrayList<>(3);
        final Map<String, String> staticArgs = new HashMap<>(3);
        public final ClasspathResource routesFile;
        public final int routesFileLine;

        private static final Pattern customRegexPattern = Pattern.compile("\\{([a-zA-Z_][a-zA-Z_0-9]*)}");
        private static final Pattern argsPattern = Pattern.compile("\\{<([^>]+)>([a-zA-Z_0-9]+)}");

        public Route(String method, String path, String action, ClasspathResource sourceFile, int line) {
            this.method = method;
            this.path = path;
            this.action = action;
            this.routesFile = sourceFile;
            this.routesFileLine = line;

            if (action.startsWith("staticDir:") || action.startsWith("staticFile:")) {
                if (!"*".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method)) {
                    throw new IllegalArgumentException("Static route only support GET method");
                }
            }
            if (action.startsWith("staticDir:")) {
                if (!path.endsWith("/")) {
                    throw new IllegalArgumentException("The path for a staticDir route must end with / : " + this);
                }
                this.pattern = Pattern.compile("^" + path + ".*$");
                this.staticFile = false;
                this.staticDir = action.substring("staticDir:".length());
                this.actionPattern = null;
            } else if (action.startsWith("staticFile:")) {
                this.pattern = isRegexp(path) ? Pattern.compile("^" + path + "$") : null;
                this.staticFile = true;
                this.staticDir = action.substring("staticFile:".length());
                this.actionPattern = null;
            } else {
                this.staticDir = null;
                this.staticFile = false;
                final String pathArguments = customRegexPattern.matcher(path).replaceAll("\\{<[^/]+>$1\\}");
                final Matcher matcher = argsPattern.matcher(pathArguments);
                while (matcher.find()) {
                    args.add(new Arg(matcher.group(2), Pattern.compile(matcher.group(1))));
                }
                this.pattern = isRegexp(path) ? Pattern.compile(pathPatternString(pathArguments)) : null;

                // Action pattern
                String actionPatternString = action.replace(".", "[.]");
                for (Arg arg : args) {
                    if (actionPatternString.contains("{" + arg.name + "}")) {
                        actionPatternString = actionPatternString.replace("{" + arg.name + "}",
                                "(?<" + arg.name + ">" + arg.constraint + ")");
                        actionArgs.add(arg.name);
                    }
                }
                actionPattern = Pattern.compile(actionPatternString, CASE_INSENSITIVE);
            }

            logger.trace("Adding [{}]", this);
        }

        static String pathPatternString(String pathArguments) {
            return argsPattern.matcher(pathArguments).replaceAll("(?<$2>$1)");
        }

        static boolean isRegexp(String path) {
            return path.contains("+") || path.contains("*") || path.contains("{");
        }

        /**
         * Check if the parts of an HTTP request equal this Route.
         *
         * @param method
         *            GET/POST/etc.
         * @param path
         *            Part after domain and before query-string. Starts with a "/".
         * @return route args or null
         */
        public Map<String, String> matches(String method, String path) {
            path = normalizePath(path);

            // If method is HEAD and we have a GET
            if (method == null || "*".equals(this.method) || method.equalsIgnoreCase(this.method)
                    || ("head".equalsIgnoreCase(method) && "get".equalsIgnoreCase(this.method))) {

                Matcher matcher = pattern == null ? null : pattern.matcher(path);

                if (this.path.equals(path) || pattern != null && matcher.matches()) {
                    if ("404".equals(action)) {
                        throw new NotFound(method, path);
                    }
                    if (staticDir != null) {
                        String resource = null;
                        if (!staticFile) {
                            resource = path.substring(this.path.length());
                        }
                        try {
                            String root = new File(staticDir).getCanonicalPath();
                            String urlDecodedResource = Utils.urlDecodePath(resource);
                            String childResourceName = staticDir + (staticFile ? "" : "/" + urlDecodedResource);
                            String child = new File(childResourceName).getCanonicalPath();
                            if (child.startsWith(root)) {
                                throw new RenderStatic(childResourceName);
                            }
                        } catch (IOException e) {
                            logger.error("Failed to render static resource for {}", this, e);
                        }
                        throw new NotFound(resource);
                    } else {
                        if (args.isEmpty() && staticArgs.isEmpty()) {
                            return emptyMap();
                        }

                        Map<String, String> localArgs = new HashMap<>(args.size() + staticArgs.size());
                        for (Arg arg : args) {
                            localArgs.put(arg.name, Utils.urlDecodePath(matcher.group(arg.name)));
                        }
                        localArgs.putAll(staticArgs);
                        return localArgs;
                    }
                }
            }
            return null;
        }

        private static String normalizePath(String path) {
            return path != null && path.isEmpty() ? "/" : path;
        }

        static class Arg {
            final String name;
            final Pattern constraint;

            Arg(String name, Pattern constraint) {
                this.name = name;
                this.constraint = constraint;
            }

            @Override
            public String toString() {
                return String.format("Arg {%s %s}", name, constraint);
            }
        }

        @Override
        public String toString() {
            return String.format("Route {%s %s %s}", method, path, action);
        }
    }

    public static class MatchingRoute {
        public final Route route;
        public final Map<String, String> args;

        public MatchingRoute(Route route, Map<String, String> args) {
            this.route = route;
            this.args = args;
        }
    }
}
