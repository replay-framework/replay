package play.mvc;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static play.mvc.Scope.Session.TS_KEY;

public class CookieSessionStoreTest {
  Http.Request request = new Http.Request();
  Http.Response response = new Http.Response();

  @Before
  public void setUp() {
    Play.configuration.setProperty(Scope.COOKIE_EXPIRATION_SETTING, "15mn");
  }

  @Test
  public void testSendOnlyIfChanged() {
    CookieSessionStore cookieSessionStore = new CookieSessionStore();
    // Mock secret
    Play.secretKey = "0112358";

    // Change nothing in the session
    Scope.Session session = cookieSessionStore.restore(request);
    cookieSessionStore.save(session, request, response);
    assertNull(response.cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));

    // Change the session
    session = cookieSessionStore.restore(request);
    session.put("username", "Bob");
    cookieSessionStore.save(session, request, response);

    Http.Cookie sessionCookie = response.cookies.get(Scope.COOKIE_PREFIX + "_SESSION");
    assertNotNull(sessionCookie);
    assertTrue(sessionCookie.value.contains("username"));
    assertTrue(sessionCookie.value.contains("Bob"));
  }

  @Test
  public void testCanRestoreSessionAfterClearingItWithoutLosingData() {
    CookieSessionStore cookieSessionStore = new CookieSessionStore();
    Play.secretKey = "0112358";
    Play.started = true;

    Scope.Session sessionFromFirstRequest = cookieSessionStore.restore(request);

    sessionFromFirstRequest.clear();
    sessionFromFirstRequest.put("param", "value");
    cookieSessionStore.save(sessionFromFirstRequest, request, response);
    request.cookies = response.cookies;

    Scope.Session sessionFromSecondRequest = cookieSessionStore.restore(request);

    assertEquals(2, sessionFromSecondRequest.data.size());
    assertEquals("value", sessionFromSecondRequest.data.get("param"));
    assertTrue(sessionFromSecondRequest.data.containsKey(TS_KEY));
  }
}