package play.mvc;

import com.lowagie.text.pdf.codec.Base64;
import org.junit.Test;
import play.PlayBuilder;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;

import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static play.mvc.Scope.Session.UA_KEY;

public class ScopeTest {

    Request request = new Request();
    Response response = new Response();

    @org.junit.Before
    public void playBuilderBefore() {
        new PlayBuilder().build();
        Scope.sessionStore = mock(SessionStore.class);
    }

    private static void mockRequestAndResponse() {
        Request.removeCurrent();
        Response.removeCurrent();
    }

    @Test
    public void testParamsPut() {
        mockRequestAndResponse();
        Params params = new Params(request);
        params.put("param1", "test");
        params.put("param1.test", "test2");

        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");

        assertEquals(6, params.all().size());

        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.object"));
        assertTrue(params._contains("param1.test"));
        assertTrue(params._contains("param1.object.param1"));
        assertTrue(params._contains("param1.object.param2"));
        assertTrue(params._contains("param1.object.param2.3"));
    }

    @Test
    public void testParamsRemove() {
        mockRequestAndResponse();
        Params params = new Params(request);
        params.put("param1", "test");
        params.put("param1.test", "test2");

        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");

        assertEquals(6, params.all().size());

        params.remove("param1.object.param2");

        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.test"));
        assertTrue(params._contains("param1.object"));
        assertTrue(params._contains("param1.object.param1"));
        assertFalse(params._contains("param1.object.param2"));
        assertTrue(params._contains("param1.object.param2.3"));

        assertEquals(5, params.all().size());
    }

    @Test
    public void testParamsRemove2() {
        mockRequestAndResponse();
        Params params = new Params(request);
        params.put("param1", "test");
        params.put("param1.test", "test2");

        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");

        assertEquals(6, params.all().size());

        params.remove("param1.object");

        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.test"));
        assertFalse(params._contains("param1.object"));
        assertTrue(params._contains("param1.object.param1"));
        assertTrue(params._contains("param1.object.param2"));
        assertTrue(params._contains("param1.object.param2.3"));

        assertEquals(5, params.all().size());
    }

    @Test
    public void testParamsRemoveStartWith() {
        mockRequestAndResponse();
        Params params = new Params(request);
        params.put("param1", "test");
        params.put("param1.test", "test2");

        params.put("param1.object", "obj");
        params.put("param1.object.param1", "param1");
        params.put("param1.object.param2", "param2");
        params.put("param1.object.param2.3", "param3");

        assertEquals(6, params.all().size());

        params.removeStartWith("param1.object");

        assertTrue(params._contains("param1"));
        assertTrue(params._contains("param1.test"));
        assertFalse(params._contains("param1.object"));
        assertFalse(params._contains("param1.object.param1"));
        assertFalse(params._contains("param1.object.param2"));
        assertFalse(params._contains("param1.object.param2.3"));

        assertEquals(2, params.all().size());
    }

    @Test
    public void sessionPutWithNullObject() {
        Session session = new Session();
        session.put("hello", (Object) null);
        assertNull(session.get("hello"));
    }

    @Test
    public void sessionPutWithObject() {
        Session session = new Session();
        session.put("hello", 123);
        assertEquals("123", session.get("hello"));
    }

    @Test
    public void sessionPutWithNullString() {
        Session session = new Session();
        session.put("hello", null);
        assertNull(session.get("hello"));
    }

    @Test
    public void sessionPutWithString() {
        Session session = new Session();
        session.put("hello", "world");
        assertEquals("world", session.get("hello"));
    }

    @Test
    public void sessionSave_storesUserAgentInSession() {
        Session session = new Session();
        session.put("hello", "world");
        request.setHeader("User-Agent", "Android; Windows Phone");

        session.save(request, new Response());

        assertEquals("Android; Windows Phone", session.get(UA_KEY));
    }

    @Test
    public void sessionSave_doesNotStoreUserAgent_ifSessionIsEmpty() {
        Session session = new Session();
        request.setHeader("User-Agent", "Android; Windows Phone");

        session.save(request, new Response());

        assertThat(session.get(UA_KEY)).isNull();
    }

    @Test
    public void sessionSave_withoutUserAgent() {
        Session session = new Session();
        session.put("hello", "world");

        session.save(request, new Response());

        assertEquals("n/a", session.get(UA_KEY));
    }

    @Test
    public void sessionSave_doesNotAddUserAgentIfAlreadyPresent() {
        Session session = spy(new Session());
        session.put(UA_KEY, "Chrome;");
        request.setHeader("User-Agent", "Chrome;");

        session.save(request, new Response());

        verify(session, times(1)).put(eq(UA_KEY), anyString());
    }

    @Test
    public void restore() {
        Session session = spy(new Session());
        session.put(UA_KEY, "Chrome;");
        request.setHeader("User-Agent", "Chrome;");
        when(Scope.sessionStore.restore(request)).thenReturn(session);

        assertThat(Session.restore(request, response)).isSameAs(session);

        verify(session, never()).clear();
        verify(Scope.sessionStore, never()).save(session, request, response);
    }

    @Test
    public void restore_throwsExceptionIfUserAgentHasChanged() {
        Session session = spy(new Session());
        session.put(UA_KEY, "Chrome;");
        request.setHeader("User-Agent", "Firefox;");
        when(Scope.sessionStore.restore(request)).thenReturn(session);

        assertThatThrownBy(() -> Session.restore(request, response))
          .isInstanceOf(ForbiddenException.class)
          .hasMessage("User agent changed: existing user agent 'Chrome;', request user agent 'Firefox;'");

        verify(session).clear();
        verify(Scope.sessionStore).save(session, request, response);
    }

    @Test
    public void restore_skipsCheckForUserAgent_ifUserAgentNotStoredYet() {
        Session session = new Session();
        request.setHeader("User-Agent", "Chrome;");
        when(Scope.sessionStore.restore(request)).thenReturn(session);

        assertThat(Session.restore(request, response)).isSameAs(session);
    }

    @Test
    public void flashErrorFormat() {
        Flash flash = new Flash();
        flash.error("Your name is %s", "Hello");

        assertEquals("Your name is Hello", flash.out.get("error"));

        flash.error("Your name is %s", "Hello %");
        assertEquals("Your name is Hello %", flash.out.get("error"));

        Messages.defaults = new Properties();
        Messages.defaults.setProperty("your.name.label", "Your name is %s");
        flash.error("your.name.label", "Hello");

        assertEquals("Your name is Hello", flash.out.get("error"));

        flash.error("your.name.label", "Hello %");
        assertEquals("Your name is Hello %", flash.out.get("error"));
    }

    @Test
    public void flashSuccessFormat() {
        Flash flash = new Flash();
        flash.success("Your name is %s", "Hello");

        assertEquals("Your name is Hello", flash.out.get("success"));

        flash.success("Your name is %s", "Hello %");
        assertEquals("Your name is Hello %", flash.out.get("success"));

        Messages.defaults = new Properties();
        Messages.defaults.setProperty("your.name.label", "Your name is %s");
        flash.success("your.name.label", "Hello");

        assertEquals("Your name is Hello", flash.out.get("success"));

        flash.success("your.name.label", "Hello %");
        assertEquals("Your name is Hello %", flash.out.get("success"));
    }

    @Test
    public void flash_save() {
        Flash flash = new Flash();
        flash.put("foo", "bar");

        flash.save(request, response);

        assertThat(response.cookies).containsKeys("PLAY_FLASH");
        String cookie = response.cookies.get("PLAY_FLASH").value;
        assertThat(cookie).isEqualTo("Zm9vPWJhcg==");
        assertThat(new String(Base64.decode(cookie), UTF_8)).isEqualTo("foo=bar");
    }
}
