package play.mvc;

import static play.mvc.Scope.*;
import static play.mvc.Scope.Session.TS_KEY;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import play.Play;
import play.exceptions.UnexpectedException;
import play.libs.Signer;
import play.libs.Time;

/** Default session store implementation that stores signed data in a cookie */
public class CookieSessionStore implements SessionStore {

  private final Signer signer = new Signer("session-");

  @Nonnull
  @Override
  public Session restore(@Nonnull Http.Request request) {
    try {
      Session session = new Session();
      Http.Cookie cookie = request.cookies.get(COOKIE_PREFIX + "_SESSION");
      long expiration = cookieLifetimeInSeconds() * 1000L;

      if (cookie != null
          && Play.started
          && cookie.value != null
          && !cookie.value.trim().isEmpty()) {
        String value = cookie.value;
        int firstDashIndex = value.indexOf('-');
        if (firstDashIndex > -1) {
          String sign = value.substring(0, firstDashIndex);
          String data = value.substring(firstDashIndex + 1);
          if (CookieDataCodec.safeEquals(sign, signer.sign(data))) {
            CookieDataCodec.decode(session.data, data);
          }
        }

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
        // no previous cookie to restore; but we may have to set the
        // timestamp in the new cookie
        session.put(TS_KEY, (System.currentTimeMillis() + expiration));
      }

      return session;
    } catch (Exception e) {
      throw new UnexpectedException("Corrupted HTTP session from " + request.remoteAddress, e);
    }
  }

  @Override
  public void save(
      @Nonnull Session session, @Nonnull Http.Request request, @Nullable Http.Response response) {
    if (response == null) {
      // Some request like WebSocket don't have any response
      return;
    }
    if (session.isEmpty()) {
      // The session is empty: delete the cookie
      if (request.cookies.containsKey(COOKIE_PREFIX + "_SESSION")) {
        response.setCookie(
            COOKIE_PREFIX + "_SESSION", "", null, "/", 0, COOKIE_SECURE, SESSION_HTTPONLY);
      }
      return;
    }
    try {
      String sessionData = CookieDataCodec.encode(session.data);
      String sign = signer.sign(sessionData);
      response.setCookie(
          COOKIE_PREFIX + "_SESSION",
          sign + "-" + sessionData,
          null,
          "/",
          cookieLifetimeInSeconds(),
          COOKIE_SECURE,
          SESSION_HTTPONLY);
    } catch (Exception e) {
      throw new UnexpectedException("Session serializationProblem", e);
    }
  }

  private int cookieLifetimeInSeconds() {
    return Time.parseDuration(Play.configuration.getProperty(Scope.COOKIE_EXPIRATION_SETTING));
  }
}
