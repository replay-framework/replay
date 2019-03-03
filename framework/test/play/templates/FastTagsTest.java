package play.templates;

import groovy.lang.Closure;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Scope.Session;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class FastTagsTest {

    private final StringWriter out = new StringWriter();
    private final String backupSystemLineBreak = System.getProperty("line.separator");
    private final ExecutableTemplate template = new ExecutableTemplate() {
        @Override public Object run() {
            return null;
        }
    };

    @Before
    public void setUp() {
        //if you render html into out
        // and expect results with line breaks
        // take into account that your tests will fail on other platforms
        // force line.separator be the same on any platform
        // or use String.format in expected code with the placeholder '%n' for any expected line separation.
        System.setProperty("line.separator","\n");
        Http.Response.setCurrent(new Http.Response());
        Http.Response.current().encoding = "UTF-8";
        template.setProperty("session", sessionWithAuthenticityToken("1234"));
    }

    private Session sessionWithAuthenticityToken(String authenticityToken) {
        Session session = new Session();
        session.put("___AT", authenticityToken);
        return session;
    }

    @After
    public void tearDown() {
        // restore line.separator
        System.setProperty("line.separator", backupSystemLineBreak);
    }

    @Test
    public void _form_simple() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("arg", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_withName() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("arg", actionDefinition);
            put("name", "my-form");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" name=\"my-form\">\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_post() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "POST";

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("arg", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
                "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_starIsPost() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.star = true;

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("arg", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
                "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_argMethodOverridesActionDefinitionMethod() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("arg", actionDefinition);
            put("method", "POST");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
                "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_customArgs() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("arg", actionDefinition);
            put("data-customer", "12");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" data-customer=\"12\" >\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_actionAsActionArg() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("action", actionDefinition);
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_customEnctype() {
        final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
        actionDefinition.url = "/foo/bar";
        actionDefinition.method = "GET";

        Map<String, ?> args = new HashMap<String, Object>() {{
            put("arg", actionDefinition);
            put("enctype", "xyz");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"xyz\" >\n" +
                "\n" +
                "</form>", out.toString());
    }

    @Test
    public void _form_argAsUrlInsteadOfActionDefinition() {
        Map<String, ?> args = new HashMap<>() {{
            put("arg", "/foo/bar");
        }};

        FastTags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

        assertEquals(
                "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
                        "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
                        "\n" +
                        "</form>", out.toString());
    }
}