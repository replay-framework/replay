package play.mvc.routing;

import org.junit.Test;
import play.mvc.routing.RoutePattern.RouteLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RoutePatternTest {
  private RoutePattern pattern = new RoutePattern();

  @Test
  public void options() {
    RouteLine matcher = pattern.matcher("OPTIONS /{<.*>any}                                                Application.corsOptions");

    assertThat(matcher.action).isEqualTo("Application.corsOptions");
    assertThat(matcher.method).isEqualTo("OPTIONS");
    assertThat(matcher.path).isEqualTo("/{<.*>any}");
  }

  @Test
  public void post() {
    RouteLine matcher = pattern.matcher("POST    /application/cspreport                                    Application.cspReport");

    assertThat(matcher.action).isEqualTo("Application.cspReport");
    assertThat(matcher.method).isEqualTo("POST");
    assertThat(matcher.path).isEqualTo("/application/cspreport");
  }

  @Test
  public void get() {
    RouteLine matcher = pattern.matcher("GET     /mobile/boot                                              mobile.Boot.index");

    assertThat(matcher.action).isEqualTo("mobile.Boot.index");
    assertThat(matcher.method).isEqualTo("GET");
    assertThat(matcher.path).isEqualTo("/mobile/boot");
  }

  @Test
  public void params() {
    RouteLine matcher = pattern.matcher("GET     /cards/{cardId}/show-requisites                           cards.Requisites.showPopup");

    assertThat(matcher.action).isEqualTo("cards.Requisites.showPopup");
    assertThat(matcher.method).isEqualTo("GET");
    assertThat(matcher.path).isEqualTo("/cards/{cardId}/show-requisites");
  }

  @Test
  public void module() {
    RouteLine matcher = pattern.matcher("*       /cms                                                      module:cms");

    assertThat(matcher.action).isEqualTo("module:cms");
    assertThat(matcher.method).isEqualTo("*");
    assertThat(matcher.path).isEqualTo("/cms");
  }

  @Test
  public void invalidRouteFormat() {
    assertThatThrownBy(() ->
      pattern.matcher("GET     /cards/{cardId}/show-requisites"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid route definition: expected 3 parts <METHOD> <PATH> <ACTION>");
  }

  @Test
  public void invalidMethod() {
    assertThatThrownBy(() ->
      pattern.matcher("GOT     /cards/{cardId}/show-requisites                           cards.Requisites.showPopup"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid route definition: unknown method GOT");
  }
}