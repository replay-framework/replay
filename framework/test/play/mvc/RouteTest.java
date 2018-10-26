package play.mvc;

import org.junit.Test;
import play.mvc.Router.Route;
import play.mvc.results.RenderStatic;

import java.util.Map;

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
    assertThat(route.args.get(0).defaultValue).isNull();

    assertThat(route.pattern.toString()).isEqualTo("/cards/({cardId}[^/]+)/requisites");
    assertThat(route.pattern.matches("/cards/1234567890/requisites")).isTrue();
    assertThat(route.pattern.matches("/cards/requisites")).isFalse();

    assertThat(route.actionArgs).isEmpty();
    assertThat(route.actionPattern.toString()).isEqualTo("cards[.]Requisites[.]showPopup");
    assertThat(route.actionPattern.matches("cards.Requisites.showPopup")).isTrue();
    assertThat(route.actionPattern.matches("cards.requisites.showpopup")).isTrue();
    assertThat(route.actionPattern.matches("cards-Requisites-showPopup")).isFalse();

    assertThat(route.matches("GET", "/cards/1234567890/requisites")).isEqualTo(Map.of("cardId", "1234567890"));
    assertThat(route.matches("GET", "/cards/requisites")).isNull();
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

    assertThat(route.pattern.toString()).isEqualTo("/auth/login");
    assertThat(route.pattern.matches("/auth/login")).isTrue();
    assertThat(route.pattern.matches("/auth/loginAndPrintReport")).isFalse();

    assertThat(route.actionArgs).isEmpty();
    assertThat(route.actionPattern.toString()).isEqualTo("com[.]blah[.]AuthController[.]doLogin");
    assertThat(route.actionPattern.matches("com.blah.authcontroller.dologin")).isTrue();
    assertThat(route.actionPattern.matches("com.blah.AuthController.do-Login")).isFalse();

    assertThat(route.matches("POST", "/auth/login")).isEqualTo(emptyMap());
    assertThat(route.matches("POST", "/cards/1234567890/requisites")).isNull();
  }

  @Test
  public void staticDir() {
    Route route = new Route("GET", "/public/", "staticDir:public", null, 0);
    assertThat(route.staticDir).isEqualTo("public");
    assertThat(route.staticFile).isFalse();
    assertThat(route.pattern.toString()).isEqualTo("^/public/.*$");
    assertThat(route.pattern.matches("/public/images/logo.gif")).isTrue();
    assertThat(route.pattern.matches("public/images/logo.gif")).isFalse();
    assertThatThrownBy(() -> route.matches("GET", "/public/images/logo.gif"))
      .isInstanceOf(RenderStatic.class)
      .satisfies(e -> assertThat(((RenderStatic) e).file).isEqualTo("public/images/logo.gif"));
  }

  @Test
  public void staticFile() {
    Route route = new Route("GET", "/robots.txt", "staticFile:/public/robots.txt", null, 0);
    assertThat(route.staticDir).isEqualTo("/public/robots.txt");
    assertThat(route.staticFile).isTrue();
    assertThat(route.pattern.toString()).isEqualTo("^/robots.txt$");
    assertThat(route.pattern.matches("/robots.txt")).isTrue();
    assertThat(route.pattern.matches("robots.txt")).isFalse();
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
}
