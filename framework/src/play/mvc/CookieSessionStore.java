package play.mvc;

import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Signer;
import play.libs.Time;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static play.mvc.Scope.COOKIE_PREFIX;
import static play.mvc.Scope.COOKIE_SECURE;
import static play.mvc.Scope.SESSION_HTTPONLY;
import static play.mvc.Scope.Session;
import static play.mvc.Scope.Session.TS_KEY;

/**
 * Default session store implementation that stores signed data in a cookie
 */
public class CookieSessionStore implements SessionStore {

    private final String COOKIE_EXPIRE = Play.configuration.getProperty(Scope.COOKIE_EXPIRATION_SETTING);
    private final Signer signer = new Signer("session-");

    @Override
    public Session restore(@Nonnull Http.Request request) {
        try {
            Session session = new Session();
            Http.Cookie cookie = request.cookies.get(COOKIE_PREFIX + "_SESSION");
            int duration = Time.parseDuration(COOKIE_EXPIRE);
            long expiration = duration * 1000L;

            if (cookie != null && Play.started && cookie.value != null && !cookie.value.trim().isEmpty()) {
                String value = cookie.value;
                int firstDashIndex = value.indexOf('-');
                if (firstDashIndex > -1) {
                    String sign = value.substring(0, firstDashIndex);
                    String data = value.substring(firstDashIndex + 1);
                    if (CookieDataCodec.safeEquals(sign, signer.sign(data))) {
                        CookieDataCodec.decode(session.data, data);
                    }
                }
                if (COOKIE_EXPIRE != null) {
                    // Verify that the session contains a timestamp, and
                    // that it's not expired
                    if (!session.contains(TS_KEY)) {
                        session = new Session();
                    } else {
                        if ((Long.parseLong(session.get(TS_KEY))) < System.currentTimeMillis()) {
                            // Session expired
                            session = new Session();
                        }
                    }
                    session.put(TS_KEY, System.currentTimeMillis() + expiration);
                } else {
                    // Just restored. Nothing changed. No cookie-expire.
                    session.changed = false;
                }
            } else {
                // no previous cookie to restore; but we may have to set the
                // timestamp in the new cookie
                if (COOKIE_EXPIRE != null) {
                    session.put(TS_KEY, (System.currentTimeMillis() + expiration));
                }
            }

            return session;
        } catch (Exception e) {
            throw new UnexpectedException("Corrupted HTTP session from " + request.remoteAddress, e);
        }
    }

    @Override
    public void save(@Nonnull Session session, @Nonnull Http.Request request, @Nullable Http.Response response) {
        if (response == null) {
            // Some request like WebSocket don't have any response
            return;
        }
        if (!session.changed && COOKIE_EXPIRE == null) {
            // Nothing changed and no cookie-expire, consequently send
            // nothing back.
            return;
        }
        if (session.isEmpty()) {
            // The session is empty: delete the cookie
            if (request.cookies.containsKey(COOKIE_PREFIX + "_SESSION")) {
                response.setCookie(COOKIE_PREFIX + "_SESSION", "", null, "/", 0, COOKIE_SECURE, SESSION_HTTPONLY);
            }
            return;
        }
        try {
            String sessionData = CookieDataCodec.encode(session.data);
            String sign = signer.sign(sessionData);
            if (COOKIE_EXPIRE == null) {
                response.setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/", null, COOKIE_SECURE,
                        SESSION_HTTPONLY);
            } else {
                response.setCookie(COOKIE_PREFIX + "_SESSION", sign + "-" + sessionData, null, "/",
                        Time.parseDuration(COOKIE_EXPIRE), COOKIE_SECURE, SESSION_HTTPONLY);
            }
        } catch (Exception e) {
            throw new UnexpectedException("Session serializationProblem", e);
        }
    }
}
