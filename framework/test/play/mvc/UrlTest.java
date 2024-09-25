package play.mvc;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableMap;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;
import play.Play;

public class UrlTest {

  @Test
  public void plainUrl() {
    assertThat(new Url("/domain/path").toString()).isEqualTo("/domain/path");
  }

  @Test
  public void toUrlWithParams_empty() {
    String actual = new Url("/foo/bar", emptyMap()).toString();
    assertThat(actual).isEqualTo("/foo/bar");
  }

  @Test
  public void toUrlWithParams_simple() {
    String actual = new Url("/foo/bar", ImmutableMap.of("a", "1")).toString();
    assertThat(actual).isEqualTo("/foo/bar?a=1");
  }

  @Test
  public void toUrlWithParams_multiple() {
    String actual = new Url("/data", ImmutableMap.of("a", "1", "b", "7")).toString();
    assertThat(actual).isEqualTo("/data?a=1&b=7");
  }

  @Test
  public void toUrlWithParams_escaping() {
    String actual = new Url("/data", ImmutableMap.of("k=ey2&", "valu ?e2")).toString();
    assertThat(actual).isEqualTo("/data?k%3Dey2%26=valu+%3Fe2");
  }

  @Test
  public void toUrlWithParams_nullValue() {
    String actual = new Url("/foo/bar", singletonMap("b", null)).toString();
    assertThat(actual).isEqualTo("/foo/bar");
  }

  @Test
  public void toUrlWithParams_nullNameThrows() {
    assertThatThrownBy(() -> new Url("url", singletonMap(null, "value")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Blank");
  }

  @Test
  public void toUrl_withNullNameThrows() {
    assertThatThrownBy(() -> new Url("url", null, "value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Blank");
  }

  @Test
  public void toUrl_withNullValueIgnoresIt() {
    String actual = new Url("/url", "name", null).toString();

    assertThat(actual).isEqualTo("/url");
  }

  @Test
  public void toUrlWithParams_urlWithParams() {
    String actual = new Url("/foo/bar?a=5", singletonMap("b", "6")).toString();

    assertThat(actual).isEqualTo("/foo/bar?a=5&b=6");
  }

  @Test
  public void toUrlWithParams_urlWithDateParam_defaultFormat() {
    Date date =
        Date.from(LocalDate.of(2000, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());

    String actual = new Url("/foo/bar", singletonMap("date", date)).toString();

    assertThat(actual).isEqualTo("/foo/bar?date=02.01.2000");
  }

  @Test
  public void toUrlWithParams_urlWithDateParam_formatFromConfig() {
    Play.configuration.setProperty("date.format", "yyyy.MM.dd");
    Date date =
        Date.from(LocalDate.of(2000, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());

    String actual = new Url("/foo/bar", singletonMap("date", date)).toString();

    assertThat(actual).isEqualTo("/foo/bar?date=2000.01.02");
  }

  @Test
  public void toUrlWith1Param() {
    String actual = new Url("/url", "name", "value").toString();

    assertThat(actual).isEqualTo("/url?name=value");
  }

  @Test
  public void toUrlWith2Params() {
    String actual = new Url("/url", "name", "value", "name2", 1).toString();

    assertThat(actual).isEqualTo("/url?name=value&name2=1");
  }

  @Test
  public void toUrlWith3Params() {
    String actual = new Url("/url", "name", "value", "name2", 1, "name3", true).toString();

    assertThat(actual).isEqualTo("/url?name=value&name2=1&name3=true");
  }

  @Test
  public void toUrlWith4Params() {
    String actual =
        new Url("/url", "name", "value", "name2", 1, "name3", true, "name4", 4L).toString();

    assertThat(actual).isEqualTo("/url?name=value&name2=1&name3=true&name4=4");
  }

  @Test
  public void toUrlWith5Params() {
    Date date =
        Date.from(LocalDate.of(2000, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

    String actual =
        new Url("/url", "name", "value", "name2", 1, "name3", true, "name4", 4L, "date", date)
            .toString();

    assertThat(actual).isEqualTo("/url?name=value&name2=1&name3=true&name4=4&date=01.01.2000");
  }

  @Test
  public void equals() {
    assertThat(new Url("/a/b")).isEqualTo(new Url("/a/b"));
  }

  @Test
  public void notEquals() {
    assertThat(new Url("/a/b")).isNotEqualTo(new Url("/a/c"));
  }

  @Test
  public void hash() {
    assertThat(new Url("/a/b").hashCode()).isEqualTo(new Url("/a/b").hashCode());
  }

  @Test
  public void differentHash() {
    assertThat(new Url("/a/b").hashCode()).isNotEqualTo(new Url("/a/c").hashCode());
  }
}
