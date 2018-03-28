package play.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.data.binding.Binder;
import play.data.parsing.DataParser;
import play.data.parsing.DataParsers;
import play.data.parsing.UrlEncodedParser;
import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.inject.Injector;
import play.libs.Codec;
import play.libs.Crypto;
import play.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.join;
import static java.util.Arrays.asList;

/**
 * All application Scopes
 */
public class Scope {
    static SessionDataEncoder encoder = new SessionDataEncoder();

    private static final Logger logger = LoggerFactory.getLogger(Scope.class);

    public static final String COOKIE_PREFIX = Play.configuration.getProperty("application.session.cookie", "PLAY");
    public static final boolean COOKIE_SECURE = "true".equals(Play.configuration.getProperty("application.session.secure", "false").toLowerCase());
    public static final String COOKIE_EXPIRATION_SETTING = "application.session.maxAge";
    public static final boolean SESSION_HTTPONLY = "true".equals(Play.configuration.getProperty("application.session.httpOnly", "false").toLowerCase());
    public static final boolean SESSION_SEND_ONLY_IF_CHANGED = "true".equals(
      Play.configuration.getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase());

    public static SessionStore sessionStore = createSessionStore();

    private static SessionStore createSessionStore() {
        String sessionStoreClass = Play.configuration.getProperty("application.session.storeClass");
        if (sessionStoreClass == null) {
            return Injector.getBeanOfType(CookieSessionStore.class);
        }

        try {
            logger.info("Storing sessions using {}", sessionStoreClass);
            return (SessionStore) Injector.getBeanOfType(sessionStoreClass);
        }
        catch (Exception e) {
            throw new UnexpectedException("Cannot create instance of " + sessionStoreClass, e);
        }
    }

    /**
     * Flash scope
     */
    public static class Flash {

        Map<String, String> data = new HashMap<>();
        Map<String, String> out = new HashMap<>();

        public static Flash restore(Http.Request request) {
            Flash flash = new Flash();
            Http.Cookie cookie = request.cookies.get(COOKIE_PREFIX + "_FLASH");
            if (cookie != null) {
                flash.data = encoder.decode(cookie.value);
            }
            return flash;
        }

        public void save(Http.Request request, Http.Response response) {
            if (request == null) {
                // Some request like WebSocket don't have any response
                return;
            }
            if (out.isEmpty()) {
                if (request.cookies.containsKey(COOKIE_PREFIX + "_FLASH") || !SESSION_SEND_ONLY_IF_CHANGED) {
                    response.setCookie(COOKIE_PREFIX + "_FLASH", "", null, "/", 0, COOKIE_SECURE, SESSION_HTTPONLY);
                }
                return;
            }
            try {
                String flashData = encoder.encode(out);
                response.setCookie(COOKIE_PREFIX + "_FLASH", flashData, null, "/", null, COOKIE_SECURE, SESSION_HTTPONLY);
            } catch (Exception e) {
                throw new UnexpectedException("Flash serializationProblem", e);
            }
        } // ThreadLocal access

        @Deprecated
        public static final ThreadLocal<Flash> current = new ThreadLocal<>();

        @Deprecated
        public static Flash current() {
            return current.get();
        }

        public void put(String key, String value) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a flash key.");
            }
            data.put(key, value);
            out.put(key, value);
        }

        public void put(String key, Object value) {
            if (value == null) {
                put(key, (String) null);
            }
            put(key, String.valueOf(value));
        }

        public void now(String key, String value) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a flash key.");
            }
            data.put(key, value);
        }

        public void error(String value, Object... args) {
            put("error", Messages.get(value, args));
        }

        public void success(String value, Object... args) {
            put("success", Messages.get(value, args));
        }

        public void discard(String key) {
            out.remove(key);
        }

        public void discard() {
            out.clear();
        }

        public void keep(String key) {
            if (data.containsKey(key)) {
                out.put(key, data.get(key));
            }
        }

        public void keep() {
            out.putAll(data);
        }

        public String get(String key) {
            return data.get(key);
        }

        public boolean remove(String key) {
            return data.remove(key) != null;
        }

        public void clear() {
            data.clear();
        }

        public boolean contains(String key) {
            return data.containsKey(key);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    /**
     * Session scope
     */
    public static class Session {

        static final String AT_KEY = "___AT";
        static final String ID_KEY = "___ID";
        static final String TS_KEY = "___TS";

        public static Session restore(Http.Request request) {
            return sessionStore.restore(request);
        }

        Map<String, String> data = new HashMap<>(); // ThreadLocal access
        boolean changed;

        @Deprecated
        public static final ThreadLocal<Session> current = new ThreadLocal<>();

        @Deprecated
        public static Session current() {
            return current.get();
        }

        public static void removeCurrent() {
            current.remove();
        }

        public String getId() {
            if (!data.containsKey(ID_KEY)) {
                this.put(ID_KEY, Codec.UUID());
            }
            return data.get(ID_KEY);

        }

        public Map<String, String> all() {
            return data;
        }

        public String getAuthenticityToken() {
            if (!data.containsKey(AT_KEY)) {
                this.put(AT_KEY, Crypto.sign(Codec.UUID()));
            }
            return data.get(AT_KEY);
        }

        void change() {
            changed = true;
        }

        public void save(Http.Request request, Http.Response response) {
            sessionStore.save(this, request, response);
        }

        public void put(String key, String value) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a session key.");
            }
            change();
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }

        public void put(String key, Object value) {
            change();
            if (value == null) {
                put(key, (String) null);
            } else {
                put(key, value.toString());
            }
        }

        public String get(String key) {
            return data.get(key);
        }

        public boolean remove(String key) {
            change();
            return data.remove(key) != null;
        }

        public void remove(String... keys) {
            for (String key : keys) {
                remove(key);
            }
        }

        public void clear() {
            change();
            data.clear();
        }

        /**
         * Returns true if the session is empty, e.g. does not contain anything else than the timestamp
         * 
         * @return true if the session is empty, otherwise false
         */
        public boolean isEmpty() {
            for (String key : data.keySet()) {
                if (!TS_KEY.equals(key)) {
                    return false;
                }
            }
            return true;
        }

        public boolean contains(String key) {
            return data.containsKey(key);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    /**
     * HTTP params
     */
    public static class Params {
        private static final ThreadLocal<Params> current = new ThreadLocal<>();

        @Deprecated
        public static Params current() {
            return current.get();
        }

        @Deprecated
        public static void setCurrent(Params params) {
            current.set(params);
        }

        public Params(Http.Request request) {
            if (request == null) {
                throw new UnexpectedException("Current request undefined");
            }
            this.request = request;
        }

        private final Http.Request request;
        private boolean requestIsParsed;
        private final Map<String, String[]> data = new LinkedHashMap<>();

        boolean rootParamsNodeIsGenerated;

        public void checkAndParse() {
            if (requestIsParsed) return;

            __mergeWith(request.routeArgs);

            if (request.querystring != null) {
                try {
                    _mergeWith(UrlEncodedParser.parseQueryString(new ByteArrayInputStream(request.querystring.getBytes(request.encoding)), request.encoding));
                }
                catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            String contentType = request.contentType;
            if (contentType != null) {
                DataParser dataParser = DataParsers.forContentType(contentType);
                if (dataParser != null) {
                    _mergeWith(dataParser.parse(request));
                }
            }
            requestIsParsed = true;
        }

        public void put(String key, String value) {
            checkAndParse();
            data.put(key, new String[] { value });
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public void put(String key, String[] values) {
            checkAndParse();
            data.put(key, values);
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public void remove(String key) {
            checkAndParse();
            data.remove(key);
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public void removeStartWith(String prefix) {
            checkAndParse();
            data.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public String get(String key) {
            checkAndParse();
            if (data.containsKey(key)) {
                return data.get(key)[0];
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        public <T> T get(Http.Request request, Annotation[] annotations, String key, Class<T> type) {
            try {
                return (T) Binder.directBind(request, annotations, get(key), type, null);
            } catch (Exception e) {
                logger.error("Failed to get {} of type {}", key, type, e);
                Validation.addError(key, "validation.invalid");
                return null;
            }
        }

        public boolean contains(String key) {
            checkAndParse();
            return data.containsKey(key);
        }

        public String[] getAll(String key) {
            checkAndParse();
            return data.get(key);
        }

        public Map<String, String[]> all() {
            checkAndParse();
            return data;
        }

        public Map<String, String[]> sub(String prefix) {
            checkAndParse();
            Map<String, String[]> result = new LinkedHashMap<>();
            for (Map.Entry<String, String[]> entry : data.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(prefix + ".")) {
                    result.put(key.substring(prefix.length() + 1), entry.getValue());
                }
            }
            return result;
        }

        public Map<String, String> allSimple() {
            checkAndParse();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, String[]> entry : data.entrySet()) {
                result.put(entry.getKey(), entry.getValue()[0]);
            }
            return result;
        }

        void _mergeWith(Map<String, String[]> map) {
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                Utils.Maps.mergeValueInMap(data, entry.getKey(), entry.getValue());
            }
        }

        void __mergeWith(Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Utils.Maps.mergeValueInMap(data, entry.getKey(), entry.getValue());
            }
        }

        public String urlEncode(Http.Response response) {
            checkAndParse();
            String encoding = response.encoding;
            StringBuilder ue = new StringBuilder();
            for (Map.Entry<String, String[]> entry : data.entrySet()) {
                if ("body".equals(entry.getKey())) {
                    continue;
                }
                String[] values = entry.getValue();
                for (String value : values) {
                    try {
                        ue.append(URLEncoder.encode(entry.getKey(), encoding)).append("=").append(URLEncoder.encode(value, encoding)).append("&");
                    } catch (Exception e) {
                        logger.error("Error (encoding ?)", e);
                    }
                }
            }
            return ue.toString();
        }

        public void flash(Flash flash, String... params) {
            Collection<String> keys = params.length == 0 ? all().keySet() : asList(params);
            for (String key : keys) {
                flash.put(key, join(",", data.get(key)));
            }
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    /**
     * Render args (used in template rendering)
     */
    public static class RenderArgs {

        public Map<String, Object> data = new HashMap<>(); // ThreadLocal access

        @Deprecated
        public static final ThreadLocal<RenderArgs> current = new ThreadLocal<>();

        @Deprecated
        public static RenderArgs current() {
            return current.get();
        }

        public static void removeCurrent() {
            current.remove();
        }

        public void put(String key, Object arg) {
            this.data.put(key, arg);
        }

        public Object get(String key) {
            return data.get(key);
        }

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> clazz) {
            return (T) this.get(key);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    /**
     * Routes args (used in reserve routing)
     */
    public static class RouteArgs {

        public Map<String, Object> data = new HashMap<>(); // ThreadLocal access

        @Deprecated
        public static final ThreadLocal<RouteArgs> current = new ThreadLocal<>();

        @Deprecated
        public static RouteArgs current() {
            return current.get();
        }

        public void put(String key, Object arg) {
            this.data.put(key, arg);
        }

        public Object get(String key) {
            return data.get(key);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }
}
