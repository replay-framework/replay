package play.mvc.routing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoutesParserTest {
  private final RoutesParser parser = new RoutesParser();

  @Test
  void removesMultipleSpaces() {
    assertThat(parser.removeMultipleSpaces("foo     bar")).isEqualTo("foo bar");
    assertThat(parser.removeMultipleSpaces("дыщ         чёрт   \t   äkki \t ökki \n õli")).isEqualTo("дыщ чёрт äkki ökki õli");
  }

  @Test
  void removesLeadingAndTrailingSpacesSpaces() {
    assertThat(parser.removeMultipleSpaces("      foo     bar      ")).isEqualTo("foo bar");
  }

  @Test
  void leavesSingleSpacesUntouched() {
    assertThat(parser.removeMultipleSpaces("foo")).isEqualTo("foo");
    assertThat(parser.removeMultipleSpaces("foo bar")).isEqualTo("foo bar");
    assertThat(parser.removeMultipleSpaces("дыщ чёрт äkki ökki õli")).isEqualTo("дыщ чёрт äkki ökki õli");
  }
}