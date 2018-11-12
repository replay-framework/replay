package play.mvc;

import org.junit.Test;
import play.mvc.Router.Route;
import play.mvc.results.RenderStatic;

import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RouteTest {
  @Test
  public void routeWithParameters() {
    Route route = new Route("GET", "/cards/{cardId}/requisites", "cards.Requisites.showPopup", null, 0);

    assertThat(route.method).isEqualTo("GET");
    assertThat(route.path).isEqualTo("/cards/{cardId}/requisites");
    assertThat(route.action).isEqualTo("cards.Requisites.showPopup");
    assertThat(route.staticDir).isNull();
    assertThat(route.staticFile).isFalse();
    assertThat(route.staticArgs).hasSize(0);

    assertThat(route.args).hasSize(1);
    assertThat(route.args.get(0).name).isEqualTo("cardId");
    assertThat(route.args.get(0).constraint.toString()).isEqualTo("[^/]+");

    assertThat(route.pattern.toString()).isEqualTo("/cards/(?<cardId>[^/]+)/requisites");
    assertThat(route.pattern.matcher("/cards/1234567890/requisites").matches()).isTrue();
    assertThat(route.pattern.matcher("/cards/requisites").matches()).isFalse();

    assertThat(route.actionArgs).isEmpty();
    assertThat(route.actionPattern.toString()).isEqualTo("cards[.]Requisites[.]showPopup");
    assertThat(route.actionPattern.matcher("cards.Requisites.showPopup").matches()).isTrue();
    assertThat(route.actionPattern.matcher("cards.requisites.showpopup").matches()).isTrue();
    assertThat(route.actionPattern.matcher("cards-Requisites-showPopup").matches()).isFalse();

    assertThat(route.matches("GET", "/cards/1234567890/requisites")).isEqualTo(Map.of("cardId", "1234567890"));
    assertThat(route.matches("GET", "/cards/requisites")).isNull();
  }

  @Test
  public void routeWithParametersInAction() {
    Route route = new Route("GET", "/news/{method}", "News.{method}", null, 0);

    assertThat(route.method).isEqualTo("GET");
    assertThat(route.path).isEqualTo("/news/{method}");
    assertThat(route.action).isEqualTo("News.{method}");
    assertThat(route.staticDir).isNull();
    assertThat(route.staticFile).isFalse();
    assertThat(route.staticArgs).hasSize(0);

    assertThat(route.args).hasSize(1);
    assertThat(route.args.get(0).name).isEqualTo("method");
    assertThat(route.args.get(0).constraint.toString()).isEqualTo("[^/]+");

    assertThat(route.pattern.toString()).isEqualTo("/news/(?<method>[^/]+)");
    assertThat(route.pattern.matcher("/news/index").matches()).isTrue();
    assertThat(route.pattern.matcher("/news/foo/bar").matches()).isFalse();

    assertThat(route.actionArgs).isEqualTo(asList("method"));
    assertThat(route.actionPattern.toString()).isEqualTo("News[.](?<method>[^/]+)");
    assertThat(route.actionPattern.matcher("news.foo").matches()).isTrue();
    assertThat(route.actionPattern.matcher("News.foo/").matches()).isFalse();

    assertThat(route.matches("GET", "/news/list")).isEqualTo(Map.of("method", "list"));
  }

  @Test
  public void routeWithoutParameters() {
    Route route = new Route("POST", "/auth/login", "com.blah.AuthController.doLogin", null, 0);

    assertThat(route.method).isEqualTo("POST");
    assertThat(route.path).isEqualTo("/auth/login");
    assertThat(route.action).isEqualTo("com.blah.AuthController.doLogin");
    assertThat(route.staticDir).isNull();
    assertThat(route.staticFile).isFalse();
    assertThat(route.staticArgs).hasSize(0);

    assertThat(route.args).hasSize(0);
    assertThat(route.pattern).isNull();

    assertThat(route.actionArgs).isEmpty();
    assertThat(route.actionPattern.toString()).isEqualTo("com[.]blah[.]AuthController[.]doLogin");
    assertThat(route.actionPattern.matcher("com.blah.authcontroller.dologin").matches()).isTrue();
    assertThat(route.actionPattern.matcher("com.blah.AuthController.do-Login").matches()).isFalse();

    assertThat(route.matches("POST", "/auth/login")).isEqualTo(emptyMap());
    assertThat(route.matches("POST", "/cards/1234567890/requisites")).isNull();
  }

  @Test
  public void optionsWithAny() {
    Route route = new Route("OPTIONS", "/{<.*>any}", "SecurityChecks.corsOptions", null, 0);

    assertThat(route.method).isEqualTo("OPTIONS");
    assertThat(route.path).isEqualTo("/{<.*>any}");
    assertThat(route.action).isEqualTo("SecurityChecks.corsOptions");
    assertThat(route.staticDir).isNull();
    assertThat(route.staticFile).isFalse();
    assertThat(route.staticArgs).hasSize(0);

    assertThat(route.args).hasSize(1);
    assertThat(route.args.get(0).name).isEqualTo("any");
    assertThat(route.args.get(0).constraint.toString()).isEqualTo(".*");

    assertThat(route.pattern.toString()).isEqualTo("/(?<any>.*)");
    assertThat(route.pattern.matcher("/").matches()).isTrue();
    assertThat(route.pattern.matcher("/robots.txt").matches()).isTrue();

    assertThat(route.actionArgs).isEmpty();
    assertThat(route.actionPattern.toString()).isEqualTo("SecurityChecks[.]corsOptions");
    assertThat(route.actionPattern.matcher("SecurityChecks.corsOptions").matches()).isTrue();
    assertThat(route.actionPattern.matcher("SecurityChecks.cors-options").matches()).isFalse();

    assertThat(route.matches("OPTIONS", "/")).isEqualTo(Map.of("any", ""));
    assertThat(route.matches("OPTIONS", "/foo")).isEqualTo(Map.of("any", "foo"));
  }

  @Test
  public void optionsWithAsterisk() {
    Route route = new Route("OPTIONS", "/.*", "SecurityChecks.corsOptions", null, 0);

    assertThat(route.method).isEqualTo("OPTIONS");
    assertThat(route.path).isEqualTo("/.*");
    assertThat(route.action).isEqualTo("SecurityChecks.corsOptions");
    assertThat(route.staticDir).isNull();
    assertThat(route.staticFile).isFalse();
    assertThat(route.staticArgs).hasSize(0);

    assertThat(route.args).hasSize(0);

    assertThat(route.pattern.toString()).isEqualTo("/.*");
    assertThat(route.pattern.matcher("/").matches()).isTrue();
    assertThat(route.pattern.matcher("/robots.txt").matches()).isTrue();

    assertThat(route.actionArgs).isEmpty();
    assertThat(route.actionPattern.toString()).isEqualTo("SecurityChecks[.]corsOptions");
    assertThat(route.actionPattern.matcher("SecurityChecks.corsOptions").matches()).isTrue();
    assertThat(route.actionPattern.matcher("SecurityChecks.cors-options").matches()).isFalse();

    assertThat(route.matches("OPTIONS", "/")).isEqualTo(emptyMap());
    assertThat(route.matches("OPTIONS", "/foo")).isEqualTo(emptyMap());
  }

  @Test
  public void staticDir() {
    Route route = new Route("GET", "/public/", "staticDir:public", null, 0);
    assertThat(route.staticDir).isEqualTo("public");
    assertThat(route.staticFile).isFalse();
    assertThat(route.pattern.toString()).isEqualTo("^/public/.*$");
    assertThat(route.pattern.matcher("/public/images/logo.gif").matches()).isTrue();
    assertThat(route.pattern.matcher("public/images/logo.gif").matches()).isFalse();
    assertThatThrownBy(() -> route.matches("GET", "/public/images/logo.gif"))
      .isInstanceOf(RenderStatic.class)
      .satisfies(e -> assertThat(((RenderStatic) e).file).isEqualTo("public/images/logo.gif"));
  }

  @Test
  public void staticFile() {
    Route route = new Route("GET", "/robots.txt", "staticFile:/public/robots.txt", null, 0);
    assertThat(route.staticDir).isEqualTo("/public/robots.txt");
    assertThat(route.staticFile).isTrue();
    assertThat(route.path).isEqualTo("/robots.txt");
    assertThat(route.pattern).isNull();
    assertThatThrownBy(() -> route.matches("GET", "/robots.txt"))
      .isInstanceOf(RenderStatic.class)
      .satisfies(e -> assertThat(((RenderStatic) e).file).isEqualTo("/public/robots.txt"));
  }

  @Test
  public void staticRouteSupportsOnlyGetMethod() {
    assertThatThrownBy(() -> new Route("POST", "/robots.txt", "staticFile:/public/robots.txt", null, 0))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Static route only support GET method");
  }

  @Test
  public void pathPatternString() {
    assertThat(Route.pathPatternString("/foo/bar")).isEqualTo("/foo/bar");
    assertThat(Route.pathPatternString("/cards/{<[^/]+>cardId}/details")).isEqualTo("/cards/(?<cardId>[^/]+)/details");
    assertThat(Route.pathPatternString("/cards/{<[^/]+>id}/activate")).isEqualTo("/cards/(?<id>[^/]+)/activate");
    assertThat(Route.pathPatternString("/file/{<[^/]+>id}")).isEqualTo("/file/(?<id>[^/]+)");
  }

  @Test
  public void isRegexp() {
    assertThat(Route.isRegexp("/.*")).isTrue();
    assertThat(Route.isRegexp("/.+")).isTrue();
    assertThat(Route.isRegexp("/cards/{cardId}/details")).isTrue();

    assertThat(Route.isRegexp("/welcome")).isFalse();
  }
}
