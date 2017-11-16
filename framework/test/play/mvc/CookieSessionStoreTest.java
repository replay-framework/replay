package play.mvc;

import org.junit.Test;
import play.Play;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class CookieSessionStoreTest {
  CookieSessionStore cookieSessionStore = new CookieSessionStore();

  @Test
  public void testSendAlways() {
    setSendOnlyIfChangedConstant(false);

    mockRequestAndResponse();

    // Change nothing in the session
    Scope.Session session = cookieSessionStore.restore();
    cookieSessionStore.save(session);
    assertNotNull(Http.Response.current().cookies.get(Scope.COOKIE_PREFIX + "_SESSION"));
  }

  @Test
  public void testSendOnlyIfChanged() {
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
    System.out.println("sessionCookie.value");
    System.out.println(sessionCookie.value);
    System.out.println("sessionCookie.value");
    assertTrue(sessionCookie.value.contains("username"));
    assertTrue(sessionCookie.value.contains("Bob"));
  }

  private static void mockRequestAndResponse() {
    Http.Request.current.set(new Http.Request());
    Http.Response.current.set(new Http.Response());
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