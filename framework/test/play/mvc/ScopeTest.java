package play.mvc;

import org.junit.After;
import org.junit.Test;
import play.PlayBuilder;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;

import java.math.BigDecimal;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static play.mvc.Scope.Session.UA_KEY;

public class ScopeTest {

    Request request = new Request();
    Response response = new Response();

    @org.junit.Before
    public void playBuilderBefore() {
        new PlayBuilder().build();
        Scope.sessionStore = mock(SessionStore.class);
    }

    @After
    public void tearDown() {
        Scope.sessionStore = null;
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

        assertTrue(params.contains("param1"));
        assertTrue(params.contains("param1.object"));
        assertTrue(params.contains("param1.test"));
        assertTrue(params.contains("param1.object.param1"));
        assertTrue(params.contains("param1.object.param2"));
        assertTrue(params.contains("param1.object.param2.3"));
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

        assertTrue(params.contains("param1"));
        assertTrue(params.contains("param1.test"));
        assertTrue(params.contains("param1.object"));
        assertTrue(params.contains("param1.object.param1"));
        assertFalse(params.contains("param1.object.param2"));
        assertTrue(params.contains("param1.object.param2.3"));

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

        assertTrue(params.contains("param1"));
        assertTrue(params.contains("param1.test"));
        assertFalse(params.contains("param1.object"));
        assertTrue(params.contains("param1.object.param1"));
        assertTrue(params.contains("param1.object.param2"));
        assertTrue(params.contains("param1.object.param2.3"));

        assertEquals(5, params.all().size());
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

        assertThat(Session.restore(request)).isSameAs(session);

        verify(session, never()).clear();
        verify(Scope.sessionStore, never()).save(session, request, response);
    }

    @Test
    public void restore_skipsCheckForUserAgent_ifUserAgentNotStoredYet() {
        Session session = new Session();
        request.setHeader("User-Agent", "Chrome;");
        when(Scope.sessionStore.restore(request)).thenReturn(session);

        assertThat(Session.restore(request)).isSameAs(session);
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
    public void flashPut() {
        Flash flash = new Flash();

        flash.put("string", "value");
        assertEquals("value", flash.get("string"));

        flash.put("integer", Integer.MAX_VALUE);
        assertEquals("2147483647", flash.get("integer"));

        flash.put("long", Long.MAX_VALUE);
        assertEquals("9223372036854775807", flash.get("long"));

        flash.put("bigDecimal", new BigDecimal("12.34"));
        assertEquals("12.34", flash.get("bigDecimal"));

        flash.put("booleanTrue", true);
        assertEquals("true", flash.get("booleanTrue"));

        flash.put("booleanFalse", false);
        assertEquals("false", flash.get("booleanFalse"));

        flash.put("enum", TestEnum.B);
        assertEquals("B", flash.get("enum"));
    }

    @Test
    public void flashPutNulls() {
        Flash flash = new Flash();

        flash.put("string", (String) null);
        assertNull(flash.get("string"));

        flash.put("integer", (Integer) null);
        assertNull(flash.get("integer"));

        flash.put("long", (Long) null);
        assertNull(flash.get("long"));

        flash.put("bigDecimal", (BigDecimal) null);
        assertNull(flash.get("bigDecimal"));

        flash.put("boolean", (Boolean) null);
        assertNull(flash.get("boolean"));
    }

    private enum TestEnum {
        A, B;

        @Override public String toString() {
            return "to string";
        }
    }

    @Test
    public void containsReturnsTrueWhenOnlyParameterNameIsQueryString() {
        request.querystring = "&name&name2";
        Params params = new Params(request);
        assertTrue(params.contains("name"));
        assertTrue(params.contains("name2"));
        assertFalse(params.contains("name3"));
    }

    @Test
    public void containsFiles() {
        request.args.put("__UPLOADS", "file");
        assertThat(request.params.containsFiles()).isTrue();
    }

    @Test
    public void containsFiles_false() {
        assertThat(request.params.containsFiles()).isFalse();
    }
}
