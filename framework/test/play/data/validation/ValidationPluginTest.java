package play.data.validation;

import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidationPluginTest {
  private final ValidationPlugin plugin = new ValidationPlugin();
  private final Error error1 = new Error("amount", "negative", emptyList());
  private final Error error2 = new Error("account", "too short", asList("foo", "bar"));

  @Test
  public void composeErrorsCookieValue() {
    assertThat(plugin.composeErrorsCookieValue(singletonList(error1)))
      .isEqualTo("[{\"message\":\"negative\",\"key\":\"amount\",\"variables\":[]}]");

    assertThat(plugin.composeErrorsCookieValue(asList(error1, error2)))
      .isEqualTo("[" +
        "{\"message\":\"negative\",\"key\":\"amount\",\"variables\":[]}," +
        "{\"message\":\"too short\",\"key\":\"account\",\"variables\":[\"foo\",\"bar\"]}" +
        "]");
  }

  @Test
  public void parseEmptyCookie() {
    assertThat(plugin.parseErrorsCookie("")).hasSize(0);
  }

  @Test
  public void parseCookieWithInvalidJson() {
    assertThat(plugin.parseErrorsCookie("{xxx=nope")).hasSize(0);
  }

  @Test
  public void parseEmptyArrayCookie() {
    assertThat(plugin.parseErrorsCookie("[]")).hasSize(0);
  }

  @Test
  public void parseCookie() {
    assertThat(plugin.parseErrorsCookie("[{\"message\":\"negative\",\"key\":\"amount\",\"variables\":[],\"severity\":0}]"))
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactly(error1);

    assertThat(plugin.parseErrorsCookie("[" +
      "{\"message\":\"too short\",\"key\":\"account\",\"variables\":[\"foo\",\"bar\"],\"severity\":42}," +
      "{\"message\":\"negative\",\"key\":\"amount\",\"variables\":[],\"severity\":0}" +
      "]"))
      .usingRecursiveFieldByFieldElementComparator()
      .containsExactly(error2, error1);
  }
}