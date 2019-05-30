package play.mvc;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.Play.Mode;
import play.exceptions.NoRouteFoundException;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.mvc.routing.RoutesParser;
import play.utils.Default;
import play.utils.Utils;
import play.vfs.VirtualFile;

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

    private void setRoutes(List<Route> routes) {
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

    public static void resetForTests(List<Route> routes) {
        instance.setRoutes(routes);
    }

    /**
     * Parse the routes file. This is called at startup.
     */
    private static void load() {
        instance.setRoutes(new RoutesParser().parse(Play.routes));
        lastLoading = System.currentTimeMillis();
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
        if (Play.routes.lastModified() > lastLoading) {
            load();
        } else {
            for (VirtualFile file : Play.modulesRoutes.values()) {
                if (file.lastModified() > lastLoading) {
                    load();
                    return;
                }
            }
        }
    }

    public void routeOnlyStatic(Http.Request request) {
        Route parameterlessRoute = findParameterlessRoute(request.method, request.path);
        if (parameterlessRoute != null) {
            try {
                if (parameterlessRoute.matches(request.method, request.path) != null) {
                    return;
                }
            } catch (RenderStatic | NotFound e) {
                throw e;
            }
        }

        for (Route route : routes) {
            try {
                if (route.matches(request.method, request.path) != null) {
                    return;
                }
            } catch (RenderStatic | NotFound e) {
                throw e;
            }
        }
    }

    Route route(Http.Request request) {
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

    private Route processRoute(Http.Request request, MatchingRoute match) {
        request.routeArgs = match.args;
        request.action = match.route.action;
        if (match.args.containsKey("format")) {
            request.format = match.args.get("format");
        }
        if (request.action.contains("{")) { // more optimization ?
            for (Map.Entry<String, String> arg : request.routeArgs.entrySet()) {
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
    public static String getFullUrl(String action, Map<String, Object> args) {
        return getFullUrl(action, args, Http.Request.current());
    }

    public static String getFullUrl(String action, Map<String, Object> args, Http.Request request) {
        ActionDefinition actionDefinition = reverse(action, args);
        String base = getBaseUrl(request);
        if (actionDefinition.method.equals("WS")) {
            return base.replaceFirst("https?", "ws") + actionDefinition;
        }
        return base + actionDefinition;
    }

    /**
     * Gets baseUrl from current request or application.baseUrl in application.conf
     */
    @Deprecated
    public static String getBaseUrl() {
        return getBaseUrl(Http.Request.current());
    }

    public static String getBaseUrl(Http.Request request) {
        if (request == null) {
            // No current request is present - must get baseUrl from config
            String appBaseUrl = Play.configuration.getProperty("application.baseUrl", "application.baseUrl");
            if (appBaseUrl.endsWith("/")) {
                // remove the trailing slash
                appBaseUrl = appBaseUrl.substring(0, appBaseUrl.length() - 1);
            }
            return appBaseUrl;

        } else {
            return request.getBase();
        }
    }

    @Deprecated
    public static String getFullUrl(String action) {
        return getFullUrl(action, Http.Request.current());
    }

    public static String getFullUrl(String action, Http.Request request) {
        // Note the map is not <code>Collections.EMPTY_MAP</code> because it
        // will be copied and changed.
        return getFullUrl(action, new HashMap<>(16), request);
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
        path = path.substring(path.indexOf("}") + 1);
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
    public static ActionDefinition reverse(String action, Map<String, Object> args) {
        return reverse(action, args, Http.Request.current(), Http.Response.current());
    }

    public static ActionDefinition reverse(String action, Map<String, Object> args, Http.Request request, Http.Response response) {
        return instance.actionToUrl(action, args, request, response);
    }

    public ActionDefinition actionToUrl(String action, Map<String, Object> actionArgs, Http.Request request, Http.Response response) {
        Map<String, Object> args = new LinkedHashMap<>(actionArgs);
        Charset encoding = response == null ? Play.defaultWebEncoding : response.encoding;

        if (action.startsWith("controllers.")) {
            action = action.substring(12);
        }
        Map<String, Object> argsbackup = new HashMap<>(args);

        String requestFormat = request == null || request.format == null ? "" : request.format;

        List<MatchingRoute> matchingRoutes = getActionRoutes(action);
        for (MatchingRoute actionRoute : matchingRoutes) {
            Route route = actionRoute.route;
            args.putAll(actionRoute.args);

            List<String> inPathArgs = new ArrayList<>(16);
            boolean allRequiredArgsAreHere = true;
            // les noms de parametres matchent ils ?
            for (Route.Arg arg : route.args) {
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
            // les parametres codes en dur dans la route matchent-ils ?
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
                for (Map.Entry<String, Object> entry : args.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (inPathArgs.contains(key) && value != null) {
                        if (List.class.isAssignableFrom(value.getClass())) {
                            @SuppressWarnings("unchecked")
                            List<Object> vals = (List<Object>) value;
                            path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", vals.get(0).toString()).replace("$", "\\$");
                        } else {
                            path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString(), encoding)
                                    .replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                            host = host.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString(), encoding)
                                    .replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                        }
                    } else if (route.staticArgs.containsKey(key)) {
                        // Do nothing -> The key is static
                    } else if (!argsbackup.containsKey(key)) {
                        // Do nothing -> The key is provided in
                        // RouteArgs and not used (see #447)
                    } else if (value != null) {
                        if (List.class.isAssignableFrom(value.getClass())) {
                            @SuppressWarnings("unchecked")
                            List<Object> vals = (List<Object>) value;
                            for (Object object : vals) {
                                queryString.append(URLEncoder.encode(key, encoding));
                                queryString.append("=");
                                String objStr = object.toString();
                                // Special case to handle jsAction
                                // tag
                                if (objStr.startsWith(":") && objStr.length() > 1) {
                                    queryString.append(':');
                                    objStr = objStr.substring(1);
                                }
                                queryString.append(URLEncoder.encode(objStr + "", encoding));
                                queryString.append("&");
                            }
                        } else if (value.getClass().equals(Default.class)) {
                            // Skip defaults in queryString
                        } else {
                            queryString.append(URLEncoder.encode(key, encoding));
                            queryString.append("=");
                            String objStr = value.toString();
                            // Special case to handle jsAction tag
                            if (objStr.startsWith(":") && objStr.length() > 1) {
                                queryString.append(':');
                                objStr = objStr.substring(1);
                            }
                            queryString.append(URLEncoder.encode(objStr + "", encoding));
                            queryString.append("&");
                        }
                    }
                }
                String qs = queryString.toString();
                if (qs.endsWith("&")) {
                    qs = qs.substring(0, qs.length() - 1);
                }
                ActionDefinition actionDefinition = new ActionDefinition();
                actionDefinition.url = qs.length() == 0 ? path : path + "?" + qs;
                actionDefinition.method = route.method == null || route.method.equals("*") ? "GET" : route.method.toUpperCase();
                actionDefinition.star = "*".equals(route.method);
                actionDefinition.action = action;
                actionDefinition.args = argsbackup;
                actionDefinition.host = host;
                if (Boolean.parseBoolean(Play.configuration.getProperty("application.forceSecureReverseRoutes", "false"))) {
                    actionDefinition.secure();
                }
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
         * Whether the route contains an astericks *.
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

        public ActionDefinition addRef(String fragment) {
            url += "#" + fragment;
            return this;
        }

        @Override
        public String toString() {
            return url;
        }

        public void absolute() {
            boolean isSecure = Http.Request.current() == null ? false : Http.Request.current().secure;
            String base = getBaseUrl();
            String hostPart = host;
            String domain = Http.Request.current() == null ? "" : Http.Request.current().get().domain;
            int port = Http.Request.current() == null ? 80 : Http.Request.current().get().port;
            if (port != 80 && port != 443) {
                hostPart += ":" + port;
            }
            // ~
            if (!url.startsWith("http")) {
                if (StringUtils.isEmpty(host)) {
                    url = base + url;
                } else if (host.contains("{_}")) {
                    Matcher matcher = Pattern.compile("([-_a-z0-9A-Z]+([.][-_a-z0-9A-Z]+)?)$")
                            .matcher(domain);
                    if (matcher.find()) {
                        url = (isSecure ? "https://" : "http://") + hostPart.replace("{_}", matcher.group(1)) + url;
                    } else {
                        url = (isSecure ? "https://" : "http://") + hostPart + url;
                    }
                } else {
                    url = (isSecure ? "https://" : "http://") + hostPart + url;
                }
                if (method.equals("WS")) {
                    url = url.replaceFirst("https?", "ws");
                }
            }
        }

        public ActionDefinition secure() {
            if (!url.contains("http://") && !url.contains("https://")) {
                absolute();
            }
            url = url.replace("http:", "https:");
            return this;
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
        public final String routesFile;
        public final int routesFileLine;

        private static final Pattern customRegexPattern = Pattern.compile("\\{([a-zA-Z_][a-zA-Z_0-9]*)}");
        private static final Pattern argsPattern = Pattern.compile("\\{<([^>]+)>([a-zA-Z_0-9]+)}");

        public Route(String method, String path, String action, String sourceFile, int line) {
            this.method = method;
            this.path = path;
            this.action = action;
            this.routesFile = sourceFile;
            this.routesFileLine = line;

            if (action.startsWith("staticDir:") || action.startsWith("staticFile:")) {
                if (!method.equalsIgnoreCase("*") && !method.equalsIgnoreCase("GET")) {
                    throw new IllegalArgumentException("Static route only support GET method");
                }
            }
            if (action.startsWith("staticDir:")) {
                if (!path.endsWith("/") && !path.equals("/")) {
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
         * Check if the parts of a HTTP request equal this Route.
         *
         * @param method
         *            GET/POST/etc.
         * @param path
         *            Part after domain and before query-string. Starts with a "/".
         * @return route args or null
         */
        public Map<String, String> matches(String method, String path) {
            // Normalize
            if ("".equals(path)) {
                path = path + "/";
            }
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
