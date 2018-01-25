package play.mvc;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.*;

public class CookieSessionStoreTest {
  @Before
  public void setUp() {
    Play.configuration.setProperty(Scope.COOKIE_EXPIRATION_SETTING, "15mn");
  }

  @Test
  public void testSendAlways() {
    CookieSessionStore cookieSessionStore = new CookieSessionStore();
    setSendOnlyIfChangedConstant(false);

    mockRequestAndResponse();

    // Change nothing in the session
    Scope.Session session = cookieSessionStore.restore();
    cookieSessionStore.save(session);
    assertNotNull(Http.Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));
  }

  @Test
  public void testSendOnlyIfChanged() {
    CookieSessionStore cookieSessionStore = new CookieSessionStore();
    // Mock secret
    Play.secretKey = "0112358";

    setSendOnlyIfChangedConstant(true);
    mockRequestAndResponse();

    // Change nothing in the session
    Scope.Session session = cookieSessionStore.restore();
    cookieSessionStore.save(session);
    assertNull(Http.Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));

    mockRequestAndResponse();
    // Change the session
    session = cookieSessionStore.restore();
    session.put("username", "Bob");
    cookieSessionStore.save(session);

    Http.Cookie sessionCookie = Http.Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION");
    assertNotNull(sessionCookie);
    assertTrue(sessionCookie.value.contains("username"));
    assertTrue(sessionCookie.value.contains("Bob"));
  }

  private static void mockRequestAndResponse() {
    Http.Request.setCurrent(new Http.Request());
    Http.Response.setCurrent(new Http.Response());
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

  @After
  public void restoreDefault() {
    boolean SESSION_SEND_ONLY_IF_CHANGED = Play.configuration.getProperty("application.session.sendOnlyIfChanged", "false").toLowerCase().equals("true");
    setSendOnlyIfChangedConstant(SESSION_SEND_ONLY_IF_CHANGED);
  }
}