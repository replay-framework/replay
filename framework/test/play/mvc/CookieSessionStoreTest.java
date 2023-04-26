package play.mvc;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import static org.assertj.core.api.Assertions.assertThat;
import static play.mvc.Scope.Session.TS_KEY;

public class CookieSessionStoreTest {
  private final Http.Request request = new Http.Request();
  private final Http.Response response = new Http.Response();

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
    assertThat(response.cookies.get(Scope.COOKIE_PREFIX + "_SESSION")).isNull();

    // Change the session
    session = cookieSessionStore.restore(request);
    session.put("username", "Bob");
    cookieSessionStore.save(session, request, response);

    Http.Cookie sessionCookie = response.cookies.get(Scope.COOKIE_PREFIX + "_SESSION");
    assertThat(sessionCookie).isNotNull();
    assertThat(sessionCookie.value.contains("username")).isTrue();
    assertThat(sessionCookie.value.contains("Bob")).isTrue();
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

    assertThat(sessionFromSecondRequest.data.size()).isEqualTo(2);
    assertThat(sessionFromSecondRequest.data.get("param")).isEqualTo("value");
    assertThat(sessionFromSecondRequest.data.containsKey(TS_KEY)).isTrue();
  }
}