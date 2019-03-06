package play.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.data.parsing.DataParser;
import play.data.parsing.DataParsers;
import play.data.parsing.UrlEncodedParser;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.inject.Injector;
import play.libs.Codec;
import play.libs.Signer;
import play.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.join;
import static java.util.Arrays.asList;

/**
 * All application Scopes
 */
public class Scope {
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
        final Map<String, String> data;
        final Map<String, String> out = new HashMap<>();

        public Flash() {
            this(new HashMap<>(2));
        }

        Flash(Map<String, String> data) {
            this.data = data;
        }

        public void put(@Nonnull String key, @Nullable String value) {
            validateKey(key);
            data.put(key, value);
            out.put(key, value);
        }

        public void put(@Nonnull String key, @Nullable Integer value) {
            put(key, value == null ? null : value.toString());
        }

        public void put(@Nonnull String key, @Nullable Long value) {
            put(key, value == null ? null : value.toString());
        }

        public void put(@Nonnull String key, @Nullable Boolean value) {
            put(key, value == null ? null : value.toString());
        }

        public void put(@Nonnull String key, @Nullable BigDecimal value) {
            put(key, value == null ? null : value.toPlainString());
        }

        public void put(@Nonnull String key, @Nonnull Enum<?> value) {
            put(key, value.name());
        }

        public void now(@Nonnull String key, @Nonnull  String value) {
            validateKey(key);
            data.put(key, value);
        }

        private void validateKey(@Nonnull String key) {
            if (key.contains(":")) {
                throw new IllegalArgumentException("Character ':' is invalid in a flash key.");
            }
        }

        public void error(@Nonnull String value, Object... args) {
            put("error", Messages.get(value, args));
        }

        public void success(@Nonnull String value, Object... args) {
            put("success", Messages.get(value, args));
        }

        public void discard(@Nonnull String key) {
            out.remove(key);
        }

        public void discard() {
            out.clear();
        }

        public void keep(@Nonnull String key) {
            if (data.containsKey(key)) {
                out.put(key, data.get(key));
            }
        }

        public void keep() {
            out.putAll(data);
        }

        @Nullable
        public String get(@Nonnull String key) {
            return data.get(key);
        }

        public boolean remove(@Nonnull String key) {
            return data.remove(key) != null;
        }

        public void clear() {
            data.clear();
        }

        public boolean contains(@Nonnull String key) {
            return data.containsKey(key);
        }

        @Override
        @Nonnull
        public String toString() {
            return data.toString();
        }
    }

    /**
     * Session scope
     */
    public static class Session {

        private static final String AT_KEY = "___AT";
        private static final String ID_KEY = "___ID";
        protected static final String TS_KEY = "___TS";
        protected static final String UA_KEY = "___UA";
        private static final Signer signer = new Signer("auth-token");

        @Nonnull
        public static Session restore(@Nonnull Http.Request request) {
            Session session = sessionStore.restore(request);
            String storedUserAgent = session.get(UA_KEY);
            String requestUserAgent = getUserAgent(request);
            if (storedUserAgent != null && !Objects.equals(requestUserAgent, storedUserAgent)) {
                logger.warn(String.format("User agent changed: existing user agent '%s', request user agent '%s'",
                                  storedUserAgent, requestUserAgent));
            }
            return session;
        }

        Map<String, String> data = new HashMap<>();
        boolean changed;

        @Nonnull
        public String getId() {
            if (!data.containsKey(ID_KEY)) {
                this.put(ID_KEY, Codec.UUID());
            }
            return data.get(ID_KEY);

        }

        @Nonnull
        public Map<String, String> all() {
            return data;
        }

        @Nonnull
        public String getAuthenticityToken() {
            if (!data.containsKey(AT_KEY)) {
                this.put(AT_KEY, signer.sign(Codec.UUID()));
            }
            return data.get(AT_KEY);
        }

        void change() {
            changed = true;
        }

        public void save(@Nonnull Http.Request request, @Nullable Http.Response response) {
            if (!isEmpty() && !contains(UA_KEY)) {
                put(UA_KEY, getUserAgent(request));
            }
            sessionStore.save(this, request, response);
        }

        @Nullable
        private static String getUserAgent(@Nonnull Http.Request request) {
            Http.Header agent = request.headers.get("user-agent");
            return agent != null ? agent.value() : "n/a";
        }

        public void put(@Nonnull String key, @Nullable String value) {
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

        public void put(@Nonnull String key, @Nullable Object value) {
            change();
            if (value == null) {
                put(key, (String) null);
            } else {
                put(key, value.toString());
            }
        }

        @Nullable
        public String get(@Nonnull String key) {
            return data.get(key);
        }

        public boolean remove(@Nonnull String key) {
            change();
            return data.remove(key) != null;
        }

        public void remove(@Nonnull String... keys) {
            for (String key : keys) {
                remove(key);
            }
        }

        public void clear() {
            String timeStamp = data.get(TS_KEY);
            change();
            data.clear();
            put(TS_KEY, timeStamp);
        }

        /**
         * Returns true if the session is empty, e.g. does not contain anything else than the timestamp
         * 
         * @return true if the session is empty, otherwise false
         */
        public boolean isEmpty() {
            for (String key : data.keySet()) {
                if (!TS_KEY.equals(key) && !UA_KEY.equals(key)) {
                    return false;
                }
            }
            return true;
        }

        public boolean contains(@Nonnull String key) {
            return data.containsKey(key);
        }

        @Override
        @Nonnull
        public String toString() {
            return data.toString();
        }
    }

    /**
     * HTTP params
     */
    public static class Params {
        public Params(@Nonnull Http.Request request) {
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

        public void put(@Nonnull String key, @Nullable String value) {
            checkAndParse();
            data.put(key, new String[] { value });
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public void put(@Nonnull String key, @Nonnull String[] values) {
            checkAndParse();
            data.put(key, values);
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        public void remove(@Nonnull String key) {
            checkAndParse();
            data.remove(key);
            // make sure rootsParamsNode is regenerated if needed
            rootParamsNodeIsGenerated = false;
        }

        @Nullable
        public String get(@Nonnull String key) {
            checkAndParse();
            if (data.containsKey(key)) {
                return data.get(key)[0];
            }
            return null;
        }

        public boolean contains(@Nonnull String key) {
            checkAndParse();
            return data.containsKey(key);
        }

        public boolean containsFiles() {
            checkAndParse();
            return request.args.containsKey("__UPLOADS");
        }

        @Nullable
        public String[] getAll(@Nonnull String key) {
            checkAndParse();
            return data.get(key);
        }

        @Nonnull
        public Map<String, String[]> all() {
            checkAndParse();
            return data;
        }

        @Nonnull
        public Map<String, String> allSimple() {
            checkAndParse();
            Map<String, String> result = new HashMap<>();
            for (Map.Entry<String, String[]> entry : data.entrySet()) {
                result.put(entry.getKey(), entry.getValue()[0]);
            }
            return result;
        }

        private void _mergeWith(@Nonnull Map<String, String[]> map) {
            for (Map.Entry<String, String[]> entry : map.entrySet()) {
                Utils.Maps.mergeValueInMap(data, entry.getKey(), entry.getValue());
            }
        }

        private void __mergeWith(@Nonnull Map<String, String> map) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                Utils.Maps.mergeValueInMap(data, entry.getKey(), entry.getValue());
            }
        }

        @Nonnull
        public String urlEncode(@Nonnull Http.Response response) {
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

        public void flash(@Nonnull Flash flash, @Nonnull String... params) {
            Collection<String> keys = params.length == 0 ? all().keySet() : asList(params);
            for (String key : keys) {
                flash.put(key, join(",", data.get(key)));
            }
        }

        @Override
        @Nonnull
        public String toString() {
            return data.toString();
        }
    }

    /**
     * Render args (used in template rendering)
     */
    public static class RenderArgs {

        public final Map<String, Object> data = new HashMap<>(); // ThreadLocal access

        @Deprecated
        public static final ThreadLocal<RenderArgs> current = new ThreadLocal<>();

        @Deprecated
        @Nonnull
        public static RenderArgs current() {
            return current.get();
        }

        public static void removeCurrent() {
            current.remove();
        }

        public void put(@Nonnull String key, @Nullable Object arg) {
            this.data.put(key, arg);
        }

        @Nullable
        public Object get(@Nonnull String key) {
            return data.get(key);
        }

        @Nullable @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> clazz) {
            return (T) this.get(key);
        }

        @Override
        @Nonnull
        public String toString() {
            return data.toString();
        }
    }
}
