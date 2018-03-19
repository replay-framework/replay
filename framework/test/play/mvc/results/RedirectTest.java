package play.mvc.results;

import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;

public class RedirectTest {
  @Test
  public void encodesParametersForUrl() {
    Map<String, Object> params = new TreeMap<>();
    params.put("телефон", "+755667788");
    params.put("descr", "га:га");
    assertThat(new Redirect("/boo/gaa", params).getUrl())
      .isEqualTo("/boo/gaa?descr=%D0%B3%D0%B0%3A%D0%B3%D0%B0&%D1%82%D0%B5%D0%BB%D0%B5%D1%84%D0%BE%D0%BD=%2B755667788");
  }
}