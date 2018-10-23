package play.mvc;

import org.junit.Test;
import play.mvc.RoutePattern.RouteMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RoutePatternTest {
  private RoutePattern pattern = new RoutePattern();

  @Test
  public void options() {
    RouteMatcher matcher = pattern.matcher("OPTIONS /{<.*>any}                                                Application.corsOptions");

    assertThat(matcher.action()).isEqualTo("Application.corsOptions");
    assertThat(matcher.method()).isEqualTo("OPTIONS");
    assertThat(matcher.path()).isEqualTo("/{<.*>any}");
  }

  @Test
  public void post() {
    RouteMatcher matcher = pattern.matcher("POST    /application/cspreport                                    Application.cspReport");

    assertThat(matcher.action()).isEqualTo("Application.cspReport");
    assertThat(matcher.method()).isEqualTo("POST");
    assertThat(matcher.path()).isEqualTo("/application/cspreport");
  }

  @Test
  public void get() {
    RouteMatcher matcher = pattern.matcher("GET     /mobile/boot                                              mobile.Boot.index");

    assertThat(matcher.action()).isEqualTo("mobile.Boot.index");
    assertThat(matcher.method()).isEqualTo("GET");
    assertThat(matcher.path()).isEqualTo("/mobile/boot");
  }

  @Test
  public void params() {
    RouteMatcher matcher = pattern.matcher("GET     /cards/{cardId}/show-requisites                           cards.Requisites.showPopup");

    assertThat(matcher.action()).isEqualTo("cards.Requisites.showPopup");
    assertThat(matcher.method()).isEqualTo("GET");
    assertThat(matcher.path()).isEqualTo("/cards/{cardId}/show-requisites");
  }

  @Test
  public void module() {
    RouteMatcher matcher = pattern.matcher("*       /cms                                                      module:cms");

    assertThat(matcher.action()).isEqualTo("module:cms");
    assertThat(matcher.method()).isEqualTo("*");
    assertThat(matcher.path()).isEqualTo("/cms");
  }

  @Test
  public void invalidMethod() {
    assertThatThrownBy(() ->
      pattern.matcher("GOT     /cards/{cardId}/show-requisites                           cards.Requisites.showPopup"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Invalid route definition");
  }
}