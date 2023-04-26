package play.plugins.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.Play;
import play.mvc.Http;
import play.mvc.NoAuthenticityToken;
import play.mvc.Scope;
import play.mvc.results.Forbidden;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AuthenticityTokenPluginTest {

  private final AuthenticityTokenPlugin plugin = new AuthenticityTokenPlugin();

  private final Http.Response response = new Http.Response();
  private final Scope.Session session = new Scope.Session();
  private final Scope.RenderArgs renderArgs = new Scope.RenderArgs();
  private final Scope.Flash flash = new Scope.Flash();

  @BeforeEach
  public void setUp() {
    Play.secretKey = "saladus";
  }

  @Test
  public void shouldNotCheckAuthenticityToken_forGetRequests() throws NoSuchMethodException {
    Http.Request request = new Http.Request();
    request.method = "GET";
    request.invokedMethod = SomeController.class.getMethod("getAction");

    plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null);
  }

  @Test
  public void shouldCheckForAuthenticityToken() throws NoSuchMethodException {
    Http.Request request = new Http.Request();
    request.method = "POST";
    request.invokedMethod = SomeController.class.getMethod("withAuthenticityTokenAction");
    request.params.put("authenticityToken", "mega-unique-uuid");
    session.put("___AT", "mega-unique-uuid");

    plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null);
  }

  @Test
  public void writesWarningInCaseOfDuplicateToken() throws NoSuchMethodException {
    Http.Request request = new Http.Request();
    request.method = "POST";
    request.invokedMethod = SomeController.class.getMethod("withAuthenticityTokenAction");
    request.params.put("authenticityToken", new String[] {"mega-unique-uuid", "mega-unique-uuid"});
    session.put("___AT", "mega-unique-uuid");

    plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null);
  }

  @Test
  public void checkForAuthenticityTokenCanBeSuppressedWithAnnotation() throws NoSuchMethodException {
    Http.Request request = new Http.Request();
    request.method = "POST";
    request.invokedMethod = SomeController.class.getMethod("noAuthenticityTokenAction");
    plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null);
  }

  @Test
  public void postRequestWithoutAuthTokenShouldBeForbidden() throws NoSuchMethodException {
    Http.Request request = new Http.Request();
    request.method = "POST";
    request.invokedMethod = SomeController.class.getMethod("withAuthenticityTokenAction");

    assertThatThrownBy(() -> plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null))
      .isInstanceOf(Forbidden.class)
      .hasMessage("No authenticity token");
  }

  @Test
  public void postRequestWithInvalidAuthTokenShouldBeForbidden() throws NoSuchMethodException {
    Http.Request request = new Http.Request();
    request.method = "POST";
    request.invokedMethod = SomeController.class.getMethod("withAuthenticityTokenAction");
    request.params.put("authenticityToken", "giga-unique-uuid");
    session.put("___AT", "mega-unique-uuid");

    assertThatThrownBy(() -> plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null))
      .isInstanceOf(Forbidden.class)
      .hasMessage("Bad authenticity token");
  }

  @Test
  public void postRequestWithMultipleAuthTokensShouldBeForbidden() throws NoSuchMethodException {
    Http.Request request = new Http.Request();
    request.method = "POST";
    request.params.put("authenticityToken", new String[] {"uuid1", "uuid1", "uuid2"});
    request.invokedMethod = SomeController.class.getMethod("withAuthenticityTokenAction");

    assertThatThrownBy(() -> plugin.beforeActionInvocation(request, response, session, renderArgs, flash, null))
      .isInstanceOf(Forbidden.class)
      .hasMessage("Multiple authenticity tokens");
  }

  private static class SomeController {
    public static void getAction() {
    }

    @NoAuthenticityToken
    public static void noAuthenticityTokenAction() {
    }

    public static void withAuthenticityTokenAction() {
    }
  }
}