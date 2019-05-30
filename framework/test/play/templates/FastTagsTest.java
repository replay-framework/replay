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
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
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
  private final FastTags tags = new FastTags(new DummyUuidGenerator("some-uuid"));

  @Before
  public void setUp() {
    //if you render html into out
    // and expect results with line breaks
    // take into account that your tests will fail on other platforms
    // force line.separator be the same on any platform
    // or use String.format in expected code with the placeholder '%n' for any expected line separation.
    System.setProperty("line.separator", "\n");
    Http.Response.setCurrent(new Http.Response());
    Http.Response.current().encoding = UTF_8;
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
    var args = Map.of("arg", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

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

    var args = Map.of(
      "arg", actionDefinition,
      "name", "my-form"
    );

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

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
    var args = Map.of("arg", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertEquals(
      "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
        "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
        "<input type=\"hidden\" name=\"___form_id\" value=\"form:some-uuid\"/>\n" +
        "\n" +
        "</form>", out.toString());
  }

  @Test
  public void _form_starIsPost() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.star = true;
    var args = Map.of("arg", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertEquals(
      "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
        "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
        "<input type=\"hidden\" name=\"___form_id\" value=\"form:some-uuid\"/>\n" +
        "\n" +
        "</form>", out.toString());
  }

  @Test
  public void _form_argMethodOverridesActionDefinitionMethod() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";

    var args = Map.of(
      "arg", actionDefinition,
      "method", "POST"
    );

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertEquals(
      "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
        "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
        "<input type=\"hidden\" name=\"___form_id\" value=\"form:some-uuid\"/>\n" +
        "\n" +
        "</form>", out.toString());
  }

  @Test
  public void _form_customArgs() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";

    var args = Map.of(
      "arg", actionDefinition,
      "data-customer", "12"
    );

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

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
    var args = Map.of("action", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

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

    var args = Map.of(
      "arg", actionDefinition,
      "enctype", "xyz"
    );

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertEquals(
      "<form action=\"/foo/bar\" method=\"get\" accept-charset=\"UTF-8\" enctype=\"xyz\" >\n" +
        "\n" +
        "</form>", out.toString());
  }

  @Test
  public void _form_argAsUrlInsteadOfActionDefinition() {
    var args = Map.of("arg", "/foo/bar");

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertEquals(
      "<form action=\"/foo/bar\" method=\"post\" accept-charset=\"UTF-8\" enctype=\"application/x-www-form-urlencoded\" >\n" +
        "<input type=\"hidden\" name=\"authenticityToken\" value=\"1234\"/>\n" +
        "<input type=\"hidden\" name=\"___form_id\" value=\"form:some-uuid\"/>\n" +
        "\n" +
        "</form>", out.toString());
  }
}