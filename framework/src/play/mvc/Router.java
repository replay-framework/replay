package play.mvc;

import jregex.Matcher;
import jregex.Pattern;
import jregex.REFlags;
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.emptyList;

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
        this.routes.addAll(routes);
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
        instance.routes.add(0, new Route(method, path, action, null, 0));
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
        for (Route route : routes) {
            try {
                if (route.matches(request.method, request.path) != null) {
                    break;
                }
            } catch (RenderStatic | NotFound e) {
                throw e;
            } catch (Throwable ignore) {
            }
        }
    }

    Route route(Http.Request request) {
        logger.trace("Route: {} - {}", request.path, request.querystring);
        for (Route route : routes) {
            Map<String, String> args = route.matches(request.method, request.path);
            if (args != null) {
                request.routeArgs = args;
                request.action = route.action;
                if (args.containsKey("format")) {
                    request.format = args.get("format");
                }
                if (request.action.contains("{")) { // more optimization ?
                    for (String arg : request.routeArgs.keySet()) {
                        request.action = request.action.replace("{" + arg + "}", request.routeArgs.get(arg));
                    }
                }
                if (request.action.equals("404")) {
                    throw new NotFound(route.path);
                }
                return route;
            }
        }
        // Not found - if the request was a HEAD, let's see if we can find a
        // corresponding GET
        if (request.method.equalsIgnoreCase("head")) {
            request.method = "GET";
            Route route = route(request);
            request.method = "HEAD";
            if (route != null) {
                return route;
            }
        }
        throw new NotFound(request.method, request.path);
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
        return reverse(action, args, Http.Request.current(), Http.Response.current(), Scope.RouteArgs.current());
    }

    public static ActionDefinition reverse(String action, Map<String, Object> args, Http.Request request, Http.Response response, Scope.RouteArgs routeArgs) {
        return instance.actionToUrl(action, args, request, response, routeArgs);
    }

    public ActionDefinition actionToUrl(String action, Map<String, Object> actionArgs, Http.Request request, Http.Response response, Scope.RouteArgs routeArgs) {
        Map<String, Object> args = new LinkedHashMap<>(actionArgs);
        String encoding = response == null ? Play.defaultWebEncoding : response.encoding;

        if (action.startsWith("controllers.")) {
            action = action.substring(12);
        }
        Map<String, Object> argsbackup = new HashMap<>(args);

        // Add routeArgs
        if (routeArgs != null) {
            for (String key : routeArgs.data.keySet()) {
                if (!args.containsKey(key)) {
                    args.put(key, routeArgs.data.get(key));
                }
            }
        }

        String requestFormat = request == null || request.format == null ? "" : request.format;

        List<ActionRoute> matchingRoutes = getActionRoutes(action);
        for (ActionRoute actionRoute : matchingRoutes) {
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
                        @SuppressWarnings("unchecked")
                        List<Object> l = (List<Object>) value;
                        value = l.get(0);
                    }
                    if (!value.toString().startsWith(":") && !arg.constraint.matches(Utils.urlEncodePath(value.toString()))) {
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
                            try {
                                path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString(), encoding)
                                        .replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                            } catch (UnsupportedEncodingException e) {
                                path = path.replaceAll("\\{(<[^>]+>)?" + key + "\\}",
                                        value.toString().replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                            }
                            try {
                                host = host.replaceAll("\\{(<[^>]+>)?" + key + "\\}", URLEncoder.encode(value.toString(), encoding)
                                        .replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                            } catch (UnsupportedEncodingException e) {
                                host = host.replaceAll("\\{(<[^>]+>)?" + key + "\\}",
                                        value.toString().replace("$", "\\$").replace("%3A", ":").replace("%40", "@").replace("+", "%20"));
                            }
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
                                try {
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
                                } catch (UnsupportedEncodingException ex) {
                                }
                            }
                        } else if (value.getClass().equals(Default.class)) {
                            // Skip defaults in queryString
                        } else {
                            try {
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
                            } catch (UnsupportedEncodingException ex) {
                            }
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

    private final Map<String, List<ActionRoute>> actionRoutesCache = new ConcurrentHashMap<>();

    private List<ActionRoute> getActionRoutes(String action) {
        return actionRoutesCache.computeIfAbsent(action, this::findActionRoutes);
    }

    private List<ActionRoute> findActionRoutes(String action) {
        List<ActionRoute> matchingRoutes = new ArrayList<>(2);
        for (Route route : routes) {
            if (route.actionPattern != null) {
                Matcher matcher = route.actionPattern.matcher(action);
                if (matcher.matches()) {
                    ActionRoute matchingRoute = new ActionRoute();
                    matchingRoute.route = route;

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

    private static final class ActionRoute {
        private Route route;
        private Map<String, String> args = new HashMap<>(2);
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
                    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([-_a-z0-9A-Z]+([.][-_a-z0-9A-Z]+)?)$")
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
        public String method;
        public String path;
        public String action;
        Pattern actionPattern;
        List<String> actionArgs = new ArrayList<>(3);
        String staticDir;
        boolean staticFile;
        Pattern pattern;
        List<Arg> args = new ArrayList<>(3);
        Map<String, String> staticArgs = new HashMap<>(3);

        public int routesFileLine;
        public String routesFile;
        static Pattern customRegexPattern = new Pattern("\\{([a-zA-Z_][a-zA-Z_0-9]*)\\}");
        static Pattern argsPattern = new Pattern("\\{<([^>]+)>([a-zA-Z_0-9]+)\\}");

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
                this.pattern = new Pattern("^" + path + ".*$");
                this.staticDir = action.substring("staticDir:".length());
            } else if (action.startsWith("staticFile:")) {
                this.pattern = new Pattern("^" + path + "$");
                this.staticFile = true;
                this.staticDir = action.substring("staticFile:".length());
            } else {
                String pathArguments = customRegexPattern.replacer("\\{<[^/]+>$1\\}").replace(path);
                Matcher matcher = argsPattern.matcher(pathArguments);
                while (matcher.find()) {
                    args.add(new Arg(matcher.group(2), new Pattern(matcher.group(1))));
                }

                String actionPatternString = argsPattern.replacer("({$2}$1)").replace(pathArguments);
                this.pattern = new Pattern(actionPatternString);

                // Action pattern
                String patternString = action.replace(".", "[.]");
                for (Arg arg : args) {
                    if (patternString.contains("{" + arg.name + "}")) {
                        patternString = patternString.replace("{" + arg.name + "}",
                                "({" + arg.name + "}" + arg.constraint + ")");
                        actionArgs.add(arg.name);
                    }
                }
                actionPattern = new Pattern(patternString, REFlags.IGNORE_CASE);
            }

            logger.trace("Adding [{}]", this);
        }

        /**
         * Check if the parts of a HTTP request equal this Route.
         *
         * @param method
         *            GET/POST/etc.
         * @param path
         *            Part after domain and before query-string. Starts with a "/".
         * @return ???
         */
        public Map<String, String> matches(String method, String path) {
            // Normalize
            if ("".equals(path)) {
                path = path + "/";
            }
            // If method is HEAD and we have a GET
            if (method == null || "*".equals(this.method) || method.equalsIgnoreCase(this.method)
                    || ("head".equalsIgnoreCase(method) && "get".equalsIgnoreCase(this.method))) {

                Matcher matcher = pattern.matcher(path);

                if (matcher.matches()) {
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
                        Map<String, String> localArgs = new HashMap<>();
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
}
