package play.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Codec;
import play.libs.Time;
import play.utils.HTTP;
import play.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;

public class Http {
    private static final Logger logger = LoggerFactory.getLogger(Http.class);

    public static final String invocationType = "HttpRequest";

    public static class StatusCode {

        public static final int OK = 200;
        public static final int CREATED = 201;
        public static final int ACCEPTED = 202;
        public static final int PARTIAL_INFO = 203;
        public static final int NO_RESPONSE = 204;
        public static final int MOVED = 301;
        public static final int FOUND = 302;
        public static final int METHOD = 303;
        public static final int NOT_MODIFIED = 304;
        public static final int BAD_REQUEST = 400;
        public static final int UNAUTHORIZED = 401;
        public static final int PAYMENT_REQUIRED = 402;
        public static final int FORBIDDEN = 403;
        public static final int NOT_FOUND = 404;
        public static final int INTERNAL_ERROR = 500;
        public static final int NOT_IMPLEMENTED = 501;
        public static final int OVERLOADED = 502;
        public static final int GATEWAY_TIMEOUT = 503;

        public static boolean success(int code) {
            return code / 100 == 2;
        }

        public static boolean redirect(int code) {
            return code / 100 == 3;
        }

        public static boolean error(int code) {
            return code / 100 == 4 || code / 100 == 5;
        }
    }

    public static class Header implements Serializable {
        public final String name;
        public final List<String> values;

        public Header(String name, String value) {
            this.name = name;
            this.values = asList(value);
        }

        public Header(String name, List<String> values) {
            this.name = name;
            this.values = values;
        }

        /**
         * First value
         * 
         * @return The first value
         */
        public String value() {
            return values.get(0);
        }

        @Override
        public String toString() {
            return name + "=" + values;
        }
    }

    public static class Cookie implements Serializable {
        public final String name;
        public String domain;
        public String path = "/";
        public boolean secure;
        public String value;
        /**
         * Cookie max-age in seconds
         */
        public Integer maxAge;
        /**
         * Don't use
         */
        public boolean sendOnError;
        public boolean httpOnly;

        public Cookie(String name, String value) {
            this.value = value;
            this.name = name;
        }
    }

    private static final ThreadLocal<Request> currentRequest = new ThreadLocal<>();

    public static class Request {
        private static final Pattern IP_REGEX = Pattern.compile("[\\s,\\d.:/a-fA-F]*");

        public String host;
        public String path;
        public String querystring;
        /**
         * URL path (excluding scheme, host and port), starting with '/'<br>
         * 
         * <b>Example:</b><br>
         * With this full URL {@code http://localhost:9000/path0/path1} <br>
         * =&gt; <b>url</b> will be {@code /path0/path1}
         */
        public String url;
        public String method;
        public String domain;
        public String remoteAddress;
        public String contentType;
        /**
         * This is the encoding used to decode this request. If encoding-info is not found in request, then
         * Play.defaultWebEncoding is used
         */
        public Charset encoding = Play.defaultWebEncoding;
        public String controller;
        public String actionMethod;
        public Integer port;
        public Boolean secure = false;
        public Map<String, Http.Header> headers;
        public Map<String, Http.Cookie> cookies;
        public transient InputStream body;
        /**
         * Additional HTTP params extracted from route
         */
        public Map<String, String> routeArgs = emptyMap();
        /**
         * Format (html,xml,json,text)
         */
        public String format;
        /**
         * Full action (ex: Application.index)
         */
        public String action;

        public Method invokedMethod;
        public Class<? extends PlayController> controllerClass;
        public PlayController controllerInstance;
        /**
         * Free space to store your request specific data
         */
        public final Map<String, Object> args = new HashMap<>(16);
        /**
         * When the request has been received
         */
        public final Date date = new Date();
        /**
         * HTTP Basic User
         */
        public String user;
        /**
         * HTTP Basic Password
         */
        public String password;
        /**
         * Request comes from loopback interface
         */
        public boolean isLoopback;
        /**
         * ActionInvoker.resolvedRoutes was called?
         */
        boolean resolved;

        @Nonnull
        public final Scope.Params params = new Scope.Params(this);

        /**
         * Deprecate the default constructor to encourage the use of createRequest() when creating new requests.
         *
         * Cannot hide it with protected because we have to be backward compatible with modules - ie
         * PlayGrizzlyAdapter.java
         */
        @Deprecated
        public Request() {
            headers = new HashMap<>(16);
            cookies = new HashMap<>(16);
        }

        public static Request createRequest(String _remoteAddress, String _method, String _path, String _querystring, String _contentType,
                InputStream _body, String _url, String _host, boolean _isLoopback, int _port, String _domain, boolean _secure,
                Map<String, Http.Header> _headers, Map<String, Http.Cookie> _cookies) {
            Request newRequest = new Request();
            newRequest.remoteAddress = _remoteAddress;
            newRequest.method = _method;
            newRequest.path = _path;
            newRequest.querystring = _querystring;

            HTTP.ContentTypeWithEncoding contentTypeEncoding = HTTP.parseContentType(_contentType);
            newRequest.contentType = contentTypeEncoding.contentType;
            newRequest.encoding = contentTypeEncoding.encoding;
            newRequest.body = _body;
            newRequest.url = _url;
            newRequest.host = _host;
            newRequest.isLoopback = _isLoopback;
            newRequest.port = _port;
            newRequest.domain = _domain;
            newRequest.secure = _secure;
            newRequest.headers = _headers != null ? _headers : new HashMap<>(16);
            newRequest.cookies = _cookies != null ? _cookies : new HashMap<>(16);
            newRequest.parseXForwarded();
            newRequest.resolveFormat();
            newRequest.authorizationInit();
            validateXForwarded(newRequest.headers.get("x-forwarded-for"));
            return newRequest;
        }

        static void validateXForwarded(Header xForwardedFor) {
            if (xForwardedFor == null) return;

            if (!IP_REGEX.matcher(xForwardedFor.value()).matches()) {
                throw new IllegalArgumentException("Unacceptable X-Forwarded-For format: " + xForwardedFor.value());
            }
        }

        private void parseXForwarded() {
            String _host = this.host;
            if (Play.configuration.containsKey("XForwardedSupport") && headers.get("x-forwarded-for") != null) {
                if (!"ALL".equalsIgnoreCase(Play.configuration.getProperty("XForwardedSupport"))
                        && !asList(Play.configuration.getProperty("XForwardedSupport", "127.0.0.1").split("[\\s,]+"))
                                .contains(remoteAddress)) {
                    throw new RuntimeException("This proxy request is not authorized: " + remoteAddress);
                } else {
                    this.secure = isRequestSecure();
                    if (Play.configuration.containsKey("XForwardedHost")) {
                        this.host = (String) Play.configuration.getProperty("XForwardedHost");
                    } else if (this.headers.get("x-forwarded-host") != null) {
                        this.host = this.headers.get("x-forwarded-host").value();
                    }
                    if (this.headers.get("x-forwarded-for") != null) {
                        this.remoteAddress = cleanupRemoteAddresses(this.headers.get("x-forwarded-for").value());
                    }
                }
            }

            if ("true".equals(Play.configuration.getProperty("XForwardedOverwriteDomainAndPort", "false").toLowerCase())
                    && this.host != null && !this.host.equals(_host)) {
                if (this.host.contains(":")) {
                    String[] hosts = this.host.split(":");
                    this.port = Integer.parseInt(hosts[1]);
                    this.domain = hosts[0];
                } else {
                    this.port = 80;
                    this.domain = this.host;
                }
            }
        }

        @Nonnull
        static String cleanupRemoteAddresses(String remoteAddress) {
            int index = remoteAddress.lastIndexOf(',');
            return index == -1 ? remoteAddress : remoteAddress.substring(index + 1).trim();
        }

        private boolean isRequestSecure() {
            Header xForwardedProtoHeader = headers.get("x-forwarded-proto");
            Header xForwardedSslHeader = headers.get("x-forwarded-ssl");
            // Check the less common "front-end-https" header,
            // used apparently only by "Microsoft Internet Security and
            // Acceleration Server"
            // and Squid when using Squid as a SSL frontend.
            Header frontEndHttpsHeader = headers.get("front-end-https");
            return ("https".equals(Play.configuration.getProperty("XForwardedProto"))
                    || (xForwardedProtoHeader != null && "https".equals(xForwardedProtoHeader.value()))
                    || (xForwardedSslHeader != null && "on".equals(xForwardedSslHeader.value()))
                    || (frontEndHttpsHeader != null && "on".equals(frontEndHttpsHeader.value().toLowerCase())));
        }

        protected void authorizationInit() {
            Header header = headers.get("authorization");
            if (header != null && header.value().startsWith("Basic ")) {
                String data = header.value().substring(6);
                // In basic auth, the password can contain a colon as well so
                // split(":") may split the string into
                // 3 parts....username, part1 of password and part2 of password
                // so don't use split here
                String decoded = new String(Codec.decodeBASE64(data), UTF_8);
                // splitting on ONLY first : allows user's password to contain a
                // :
                int indexOf = decoded.indexOf(':');
                if (indexOf < 0)
                    return;

                String username = decoded.substring(0, indexOf);
                String thePasswd = decoded.substring(indexOf + 1);
                user = !username.isEmpty() ? username : null;
                password = !thePasswd.isEmpty() ? thePasswd : null;
            }
        }

        /**
         * Automatically resolve request format from the Accept header (in this order : html &gt; xml &gt; json &gt;
         * text)
         */
        public void resolveFormat() {

            if (format != null) {
                return;
            }

            if (headers.get("accept") == null) {
                format = "html";
                return;
            }

            String accept = headers.get("accept").value();

            if (accept.contains("application/xhtml") || accept.contains("text/html") || accept.startsWith("*/*")) {
                format = "html";
                return;
            }

            if (accept.contains("application/xml") || accept.contains("text/xml")) {
                format = "xml";
                return;
            }

            if (accept.contains("text/plain")) {
                format = "txt";
                return;
            }

            if (accept.contains("application/json") || accept.contains("text/javascript")) {
                format = "json";
                return;
            }

            if (accept.endsWith("*/*")) {
                format = "html";
                return;
            }
        }

        /**
         * Retrieve the current request
         * 
         * @return the current request
         */
        @Deprecated
        public static Request current() {
            return currentRequest.get();
        }

        @Deprecated
        public static void setCurrent(Request request) {
            currentRequest.set(request);
        }

        public static void removeCurrent() {
            currentRequest.remove();
        }

        /**
         * This request was sent by an Ajax framework. (rely on the X-Requested-With header).
         * 
         * @return True is the request is an Ajax, false otherwise
         */
        public boolean isAjax() {
            Header header = headers.get("x-requested-with");
            return header != null && "XMLHttpRequest".equals(header.value());
        }

        @Nullable
        public String getUserAgent() {
            Http.Header agent = headers.get("user-agent");
            return agent != null ? agent.value() : "n/a";
        }

        /**
         * Get the request base (ex: http://localhost:9000
         * 
         * @return the request base of the url (protocol, host and port)
         */
        public String getBase() {
            if (port == 80 || port == 443) {
                return String.format("%s://%s", secure ? "https" : "http", domain).intern();
            }
            return String.format("%s://%s:%s", secure ? "https" : "http", domain, port).intern();
        }

        @Override
        public String toString() {
            return method + " " + path + (querystring != null && !querystring.isEmpty() ? "?" + querystring : "");
        }

        /**
         * Return the languages requested by the browser, ordered by preference (preferred first). If no Accept-Language
         * header is present, an empty list is returned.
         *
         * @return Language codes in order of preference, e.g. "en-us,en-gb,en,de".
         */
        public List<String> acceptLanguage() {
            final Pattern qpattern = Pattern.compile("q=([0-9.]+)");
            if (!headers.containsKey("accept-language")) {
                return Collections.emptyList();
            }
            String acceptLanguage = headers.get("accept-language").value();
            List<String> languages = asList(acceptLanguage.split(","));
            languages.sort((lang1, lang2) -> {
                double q1 = 1.0;
                double q2 = 1.0;
                Matcher m1 = qpattern.matcher(lang1);
                Matcher m2 = qpattern.matcher(lang2);
                if (m1.find()) {
                    q1 = Double.parseDouble(m1.group(1));
                }
                if (m2.find()) {
                    q2 = Double.parseDouble(m2.group(1));
                }
                return (int) (q2 - q1);
            });
            List<String> result = new ArrayList<>(10);
            for (String lang : languages) {
                result.add(lang.trim().split(";")[0]);
            }
            return result;
        }

        public boolean isModified(String etag, long last) {
            if (!(headers.containsKey("if-none-match") && headers.containsKey("if-modified-since"))) {
                return true;
            } else {
                String browserEtag = headers.get("if-none-match").value();
                if (!browserEtag.equals(etag)) {
                    return true;
                } else {
                    try {
                        Date browserDate = Utils.getHttpDateFormatter().parse(headers.get("if-modified-since").value());
                        if (browserDate.getTime() >= last) {
                            return false;
                        }
                    } catch (ParseException ex) {
                        logger.error("Can't parse date", ex);
                    }
                    return true;
                }
            }
        }

        public void setCookie(String key, String value) {
            cookies.put(key, new Http.Cookie(key, value));
        }

        public void setHeader(String key, String value) {
            key = key.toLowerCase();
            headers.put(key, new Http.Header(key, value));
        }

        public <T extends Annotation> T getActionAnnotation(Class<T> annotationClass) {
            T annotation = invokedMethod.getAnnotation(annotationClass);
            if (annotation == null) {
                annotation = controllerClass.getAnnotation(annotationClass);
            }
            return annotation;
        }
    }

    private static final ThreadLocal<Response> currentResponse = new ThreadLocal<>();

    public static class Response {
        public Integer status = StatusCode.OK;
        public String contentType;
        public final Map<String, Http.Header> headers = new HashMap<>(16);
        public Map<String, Http.Cookie> cookies = new HashMap<>(16);
        public ByteArrayOutputStream out;
        public Object direct;
        public Charset encoding = Play.defaultWebEncoding;

        @Deprecated
        public static Response current() {
            return currentResponse.get();
        }

        @Deprecated
        public static void setCurrent(Response response) {
            currentResponse.set(response);
        }

        public static void removeCurrent() {
            currentResponse.remove();
        }

        /**
         * Get a response header
         * 
         * @param name
         *            Header name case-insensitive
         * @return the header value as a String
         */
        @Nullable
        public String getHeader(@Nonnull String name) {
            for (Map.Entry<String, Header> entry : headers.entrySet()) {
                if (entry.getKey().toLowerCase().equals(name.toLowerCase())) {
                    if (entry.getValue() != null) {
                        return entry.getValue().value();
                    }
                }
            }
            return null;
        }

        public void setHeader(String name, String value) {
            headers.put(name, new Header(name, value));
        }

        public void setContentTypeIfNotSet(String contentType) {
            if (this.contentType == null) {
                this.contentType = contentType;
            }
        }

        public void setCookie(String name, String value) {
            setCookie(name, value, null, "/", null, false);
        }

        public void removeCookie(String name) {
            removeCookie(name, "/");
        }

        public void removeCookie(String name, String path) {
            setCookie(name, "", null, path, 0, false);
        }

        /**
         * Set a new cookie that will expire in (current) + duration
         * @param duration the cookie duration (Ex: "3d")
         */
        public void setCookie(String name, String value, String duration) {
            setCookie(name, value, null, "/", Time.parseDuration(duration), false);
        }

        public void setCookie(String name, String value, String domain, String path, Integer maxAge, boolean secure) {
            setCookie(name, value, domain, path, maxAge, secure, false);
        }

        public void setCookie(String name, String value, String domain, String path, Integer maxAge, boolean secure, boolean httpOnly) {
            if (cookies.containsKey(name) && cookies.get(name).path.equals(path)
                    && ((cookies.get(name).domain == null && domain == null) || (cookies.get(name).domain.equals(domain)))) {
                cookies.get(name).value = value;
                cookies.get(name).maxAge = maxAge;
                cookies.get(name).secure = secure;
            } else {
                Cookie cookie = new Cookie(name, value);
                cookie.path = path;
                cookie.secure = secure;
                cookie.httpOnly = httpOnly;
                if (domain != null) {
                    cookie.domain = domain;
                }
                if (maxAge != null) {
                    cookie.maxAge = maxAge;
                }
                cookies.put(name, cookie);
            }
        }

        /**
         * Add a cache-control header
         * 
         * @param duration
         *            Ex: 3h
         */
        public void cacheFor(String duration) {
            int maxAge = Time.parseDuration(duration);
            setHeader("Cache-Control", "max-age=" + maxAge);
        }

        /**
         * Add cache-control headers
         * 
         * @param etag
         *            the Etag value
         * 
         * @param duration
         *            the cache duration (Ex: 3h)
         * @param lastModified
         *            The last modified date
         */
        public void cacheFor(String etag, String duration, long lastModified) {
            int maxAge = Time.parseDuration(duration);
            setHeader("Cache-Control", "max-age=" + maxAge);
            setHeader("Last-Modified", Utils.getHttpDateFormatter().format(new Date(lastModified)));
            setHeader("Etag", etag);
        }

        /**
         * Add headers to allow cross-domain requests. Be careful, a lot of browsers don't support these features and
         * will ignore the headers. Refer to the browsers' documentation to know what versions support them.
         * 
         * @param allowOrigin
         *            a comma separated list of domains allowed to perform the x-domain call, or "*" for all.
         */
        public void accessControl(String allowOrigin) {
            accessControl(allowOrigin, null, false);
        }

        /**
         * Add headers to allow cross-domain requests. Be careful, a lot of browsers don't support these features and
         * will ignore the headers. Refer to the browsers' documentation to know what versions support them.
         * 
         * @param allowOrigin
         *            a comma separated list of domains allowed to perform the x-domain call, or "*" for all.
         * @param allowCredentials
         *            Let the browser send the cookies when doing a x-domain request. Only respected by the browser if
         *            allowOrigin != "*"
         */
        public void accessControl(String allowOrigin, boolean allowCredentials) {
            accessControl(allowOrigin, null, allowCredentials);
        }

        /**
         * Add headers to allow cross-domain requests. Be careful, a lot of browsers don't support these features and
         * will ignore the headers. Refer to the browsers' documentation to know what versions support them.
         * 
         * @param allowOrigin
         *            a comma separated list of domains allowed to perform the x-domain call, or "*" for all.
         * @param allowMethods
         *            a comma separated list of HTTP methods allowed, or null for all.
         * @param allowCredentials
         *            Let the browser send the cookies when doing a x-domain request. Only respected by the browser if
         *            allowOrigin != "*"
         */
        public void accessControl(String allowOrigin, String allowMethods, boolean allowCredentials) {
            setHeader("Access-Control-Allow-Origin", allowOrigin);
            if (allowMethods != null) {
                setHeader("Access-Control-Allow-Methods", allowMethods);
            }
            if (allowCredentials) {
                if ("*".equals(allowOrigin)) {
                    logger.warn(
                            "Response.accessControl: When the allowed domain is \"*\", Allow-Credentials is likely to be ignored by the browser.");
                }
                setHeader("Access-Control-Allow-Credentials", "true");
            }
        }

        public void print(String text) {
            try {
                out.write(text.getBytes(encoding));
            } catch (IOException ex) {
                throw new UnexpectedException("Failed to print response '" + text + "'", ex);
            }
        }

        public boolean chunked;
        private final List<Consumer<Object>> writeChunkHandlers = new ArrayList<>();

        public void writeChunk(Object o) {
            this.chunked = true;
            if (writeChunkHandlers.isEmpty()) {
                throw new UnsupportedOperationException("Your HTTP server doesn't yet support chunked response stream");
            }
            for (Consumer<Object> handler : writeChunkHandlers) {
                handler.accept(o);
            }
        }

        public void onWriteChunk(Consumer<Object> handler) {
            writeChunkHandlers.add(handler);
        }
    }
}
