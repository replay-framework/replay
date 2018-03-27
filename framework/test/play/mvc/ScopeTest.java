package play.mvc;

import org.junit.Test;
import play.PlayBuilder;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;

import java.util.Properties;

import static org.junit.Assert.*;

public class ScopeTest {

    Request request = new Request();

    @org.junit.Before
    public void playBuilderBefore() {
        new PlayBuilder().build();
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

        assertTrue(params.data.containsKey("param1"));
        assertTrue(params.data.containsKey("param1.object"));
        assertTrue(params.data.containsKey("param1.test"));
        assertTrue(params.data.containsKey("param1.object.param1"));
        assertTrue(params.data.containsKey("param1.object.param2"));
        assertTrue(params.data.containsKey("param1.object.param2.3"));
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

        assertTrue(params.data.containsKey("param1"));
        assertTrue(params.data.containsKey("param1.test"));
        assertTrue(params.data.containsKey("param1.object"));
        assertTrue(params.data.containsKey("param1.object.param1"));
        assertFalse(params.data.containsKey("param1.object.param2"));
        assertTrue(params.data.containsKey("param1.object.param2.3"));

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

        assertTrue(params.data.containsKey("param1"));
        assertTrue(params.data.containsKey("param1.test"));
        assertFalse(params.data.containsKey("param1.object"));
        assertTrue(params.data.containsKey("param1.object.param1"));
        assertTrue(params.data.containsKey("param1.object.param2"));
        assertTrue(params.data.containsKey("param1.object.param2.3"));

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

        assertTrue(params.data.containsKey("param1"));
        assertTrue(params.data.containsKey("param1.test"));
        assertFalse(params.data.containsKey("param1.object"));
        assertFalse(params.data.containsKey("param1.object.param1"));
        assertFalse(params.data.containsKey("param1.object.param2"));
        assertFalse(params.data.containsKey("param1.object.param2.3"));

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
    public void containsReturnsTrueWhenOnlyParameterNameIsQueryString() {
        request.querystring = "&name&name2";
        Params params = new Params(request);
        assertTrue(params.contains("name"));
        assertTrue(params.contains("name2"));
        assertFalse(params.contains("name3"));
    }
}
