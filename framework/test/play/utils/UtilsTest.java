package play.utils;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static play.utils.Utils.getHttpDateFormatter;

public class UtilsTest {
  @Test
  public void join() {
    assertThat(Utils.join(new String[] {"a", "b"}, "; ")).isEqualTo("a; b");
    assertThat(Utils.join(new String[] {"a"}, "; ")).isEqualTo("a");
    assertThat(Utils.join(new String[] {""}, "; ")).isEqualTo("");
    assertThat(Utils.join(new String[0], "; ")).isEqualTo("");
  }

  @Test
  public void httpDateFormatter() throws ParseException {
    Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse("1981-06-08 23:59:58 +0700");
    assertThat(getHttpDateFormatter().format(date)).isEqualTo("Mon, 08 Jun 1981 16:59:58 GMT");
  }

  @Test
  public void urlDecodePath() {
    assertThat(Utils.urlDecodePath("images/logo.gif")).isEqualTo("images/logo.gif");
    assertThat(Utils.urlDecodePath("images%2Flogo.gif")).isEqualTo("images/logo.gif");
    assertThat(Utils.urlDecodePath("1234567890")).isEqualTo("1234567890");
    assertThat(Utils.urlDecodePath("")).isEqualTo("");
    assertThat(Utils.urlDecodePath(null)).isNull();
  }

  @Test
  public void urlEncodePath() {
    assertThat(Utils.urlEncodePath("images/logo.gif")).isEqualTo("images%2Flogo.gif");
    assertThat(Utils.urlEncodePath("somepackage.mycontroller")).isEqualTo("somepackage.mycontroller");
    assertThat(Utils.urlEncodePath("987654321")).isEqualTo("987654321");
    assertThat(Utils.urlEncodePath("/foo&bar'baz")).isEqualTo("%2Ffoo%26bar%27baz");
    assertThat(Utils.urlEncodePath("")).isEqualTo("");
  }

  @Test
  public void mergeValueInMap_name_values() {
    Map<String, String[]> map = new HashMap<>();
    Utils.Maps.mergeValueInMap(map, "firstName", new String[] {"Bob", "John"});
    assertThat(map).containsExactlyEntriesOf(Map.of("firstName", new String[] {"Bob", "John"}));
  }

  @Test
  public void mergeValueInMap_name_value() {
    Map<String, String[]> map = new HashMap<>();
    Utils.Maps.mergeValueInMap(map, "firstName", (String) null);
    assertThat(map).containsOnlyKeys("firstName");
    assertThat(map).containsExactlyEntriesOf(Map.of("firstName", new String[] {null}));
  }
}