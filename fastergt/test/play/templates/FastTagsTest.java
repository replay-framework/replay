package play.templates;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.Scope.Session;
import play.utils.DummyUuidGenerator;

public class FastTagsTest {

  private final StringWriter out = new StringWriter();
  private final ExecutableTemplate template =
      new ExecutableTemplate() {
        @Override
        public Object run() {
          return null;
        }
      };
  private final FastTags tags = new FastTags(new DummyUuidGenerator("some-uuid"));
  private final Closure body = mock(Closure.class);

  @BeforeEach
  public void setUp() {
    //if you render html into out
    // and expect results with line breaks
    // take into account that your tests will fail on other platforms
    // force line.separator be the same on any platform
    // or use String.format in expected code with the placeholder '%n' for any expected line separation.
    Http.Response.setCurrent(new Http.Response());
    Http.Response.current().encoding = UTF_8;
    template.setProperty("session", sessionWithAuthenticityToken("1234"));
  }

  private Session sessionWithAuthenticityToken(String authenticityToken) {
    Session session = new Session();
    session.put("___AT", authenticityToken);
    return session;
  }

  @Test
  public void _form_simple() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";
    var args = Map.of("arg", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="get" accept-charset="UTF-8" enctype="application/x-www-form-urlencoded" >
        </form>""");
  }

  @Test
  public void _form_withName() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";

    var args = Map.of("arg", actionDefinition, "name", "my-form");

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="get"
         accept-charset="UTF-8" enctype="application/x-www-form-urlencoded"
         name="my-form">
        </form>
        """);
  }

  @Test
  public void _form_post() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "POST";
    var args = Map.of("arg", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="post" accept-charset="UTF-8" enctype="application/x-www-form-urlencoded" >
        <input type="hidden" name="authenticityToken" value="1234"/>
        <input type="hidden" name="___form_id" value="form:some-uuid"/>
        </form>
        """);
  }

  @Test
  public void _form_starIsPost() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.star = true;
    var args = Map.of("arg", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="post" accept-charset="UTF-8" enctype="application/x-www-form-urlencoded" >
        <input type="hidden" name="authenticityToken" value="1234"/>
        <input type="hidden" name="___form_id" value="form:some-uuid"/>
        </form>
        """);
  }

  @Test
  public void _form_argMethodOverridesActionDefinitionMethod() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";

    var args = Map.of("arg", actionDefinition, "method", "POST");

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="post" accept-charset="UTF-8" enctype="application/x-www-form-urlencoded" >
        <input type="hidden" name="authenticityToken" value="1234"/>
        <input type="hidden" name="___form_id" value="form:some-uuid"/>
        </form>
        """);
  }

  @Test
  public void _form_customArgs() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";

    var args = Map.of("arg", actionDefinition, "data-customer", "12");

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="get" accept-charset="UTF-8" enctype="application/x-www-form-urlencoded" data-customer="12" >
        </form>
        """);
  }

  @Test
  public void _form_actionAsActionArg() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";
    var args = Map.of("action", actionDefinition);

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="get" accept-charset="UTF-8" enctype="application/x-www-form-urlencoded" >
        </form>
        """);
  }

  @Test
  public void _form_customEnctype() {
    final Router.ActionDefinition actionDefinition = new Router.ActionDefinition();
    actionDefinition.url = "/foo/bar";
    actionDefinition.method = "GET";

    var args = Map.of("arg", actionDefinition, "enctype", "xyz");

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="get" accept-charset="UTF-8" enctype="xyz" >
        </form>
        """);
  }

  @Test
  public void _form_argAsUrlInsteadOfActionDefinition() {
    var args = Map.of("arg", "/foo/bar");

    tags._form(args, mock(Closure.class), new PrintWriter(out), template, 0);

    assertThat(out.toString()).isEqualToIgnoringNewLines("""
        <form action="/foo/bar" method="post" accept-charset="UTF-8" enctype="application/x-www-form-urlencoded" >
        <input type="hidden" name="authenticityToken" value="1234"/>
        <input type="hidden" name="___form_id" value="form:some-uuid"/>
        </form>
        """);
  }

  @Test
  public void getValue_noArg() {
    assertThat(tags.getValue(body, "")).isEmpty();
    verifyNoMoreInteractions(body);
  }

  @Test
  public void getValue_simpleArg() {
    when(body.getProperty(anyString())).thenReturn("42.99");

    assertThat(tags.getValue(body, "amount")).hasValue("42.99");

    verify(body).getProperty("amount");
    verifyNoMoreInteractions(body);
  }

  @Test
  public void getValue_simpleArg_butMissingProperty() {
    when(body.getProperty(anyString())).thenReturn(null);

    assertThat(tags.getValue(body, "width")).isEmpty();

    verify(body).getProperty("width");
    verifyNoMoreInteractions(body);
  }

  @Test
  public void getValue_argWithDots() {
    when(body.getProperty(anyString())).thenReturn(new Payment(new BigDecimal("42.99")));

    assertThat(tags.getValue(body, "payment.amount")).hasValue(new BigDecimal("42.99"));

    verify(body).getProperty("payment");
    verifyNoMoreInteractions(body);
  }

  @Test
  public void getValue_argWithDots_butMissingProperty() {
    when(body.getProperty(anyString())).thenReturn(new Payment(new BigDecimal("42.99")));

    assertThat(tags.getValue(body, "payment.height")).isEmpty();

    verify(body).getProperty("payment");
    verifyNoMoreInteractions(body);
  }

  public static final class Payment {

    private final BigDecimal amount;

    private Payment(BigDecimal amount) {
      this.amount = amount;
    }

    public BigDecimal getAmount() {
      return amount;
    }
  }
}
