package play.mvc;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;
import static play.mvc.Scope.Session.TS_KEY;

public class CookieSessionStoreTest {
  Http.Request request = new Http.Request();
  Http.Response response = new Http.Response();

  @Before
  public void setUp() {
    Play.configuration.setProperty(Scope.COOKIE_EXPIRATION_SETTING, "15mn");
  }

  @Test
  public void testSendAlways() {
    CookieSessionStore cookieSessionStore = new CookieSessionStore();
    setSendOnlyIfChangedConstant(false);

    // Change nothing in the session
    Scope.Session session = cookieSessionStore.restore(request);
    cookieSessionStore.save(session, request, response);
    assertNotNull(response.cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));
  }

  @Test
  public void testSendOnlyIfChanged() {
    CookieSessionStore cookieSessionStore = new CookieSessionStore();
    // Mock secret
    Play.secretKey = "0112358";

    setSendOnlyIfChangedConstant(true);

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
  public void testCanRestoreSessionAfterClearingItWithoutLosingData() throws Exception {
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

  private void setSendOnlyIfChangedConstant(boolean value) {
    try {
      /*
       * Set the final static value Scope.SESSION_SEND_ONLY_IF_CHANGED using reflection.
       */
      Field field = Scope.class.getField("SESSION_SEND_ONLY_IF_CHANGED");
      field.setAccessible(true);
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      modifiersField.setAccessible(true);
      modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

      // Set the new value
      field.setBoolean(null, value);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  @org.junit.After
  public void restoreDefault() {
    boolean SESSION_SEND_ONLY_IF_CHANGED = Play.configuration.getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase().equals("true");
    setSendOnlyIfChangedConstant(SESSION_SEND_ONLY_IF_CHANGED);
  }
}