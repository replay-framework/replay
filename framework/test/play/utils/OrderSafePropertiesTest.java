package play.utils;

import org.junit.Test;
import play.libs.IO;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static play.utils.OrderSafeProperties.removeEscapedBackslashes;
import static play.utils.OrderSafeProperties.unescapeQuotes;

public class OrderSafePropertiesTest {
  @Test
  public void verifyThatEscaping_properties_content_giveSameResultAs_java_util_properties() throws IOException {
    // see info about escaping - http://download.oracle.com/javase/1.5.0/docs/api/java/util/Properties.html - "public void load(InputStream inStream)"
    Properties javaP = readJavaProperties("/play/utils/OrderSaferPropertiesTest2.properties");
    Properties playP = IO.readUtf8Properties("/play/utils/OrderSaferPropertiesTest2.properties");
    assertThat(playP.getProperty("a")).isEqualTo(javaP.getProperty("a"));
  }

  @Test
  public void unescapeQuotes_replacesEscapedQuoteByJustQuote() {
    assertThat(unescapeQuotes("\\\" ")).isEqualTo("\" ");
    assertThat(unescapeQuotes("\\\" quoted string \\\"")).isEqualTo("\" quoted string \"");
  }

  @Test
  public void unescapeQuotes_replacesEscapedAphostropheByJustAphostrophe() {
    assertThat(unescapeQuotes("'quoted string'")).isEqualTo("'quoted string'");
    assertThat(unescapeQuotes("\\'quoted string\\'")).isEqualTo("'quoted string'");
  }

  @Test
  public void removeEscapedBackslashes_xx() {
    assertThat(removeEscapedBackslashes("'quoted string'")).isEqualTo("'quoted string'");
    assertThat(removeEscapedBackslashes("\\\\\"quote \\'apostrophe")).isEqualTo("\\\"quote \\'apostrophe");
    assertThat(removeEscapedBackslashes("\\\\\\\"quote \\\\'apostrophe")).isEqualTo("\\\\\"quote \\'apostrophe");
    assertThat(removeEscapedBackslashes("\\\\\\\\\"quote \\\\\\'apostrophe")).isEqualTo("\\\\\"quote \\\\'apostrophe");
  }

  @Test
  public void verifyCorrectOrder_usingKeySet() throws IOException {
    Properties p = IO.readUtf8Properties("/play/utils/OrderSaferPropertiesTest.properties");

    int order = 0;
    for (Object _key : p.keySet()) {
      String key = (String) _key;
      if (!key.startsWith("_")) {
        String value = p.getProperty(key);
        int currentOrder = Integer.parseInt(value);
        order++;
        assertThat(currentOrder).isEqualTo(order);
      }
    }
  }

  @Test
  public void verifyCorrectOrder_usingEntrySet() throws IOException {
    Properties p = IO.readUtf8Properties("/play/utils/OrderSaferPropertiesTest.properties");
    int order = 0;
    for (Map.Entry<Object, Object> e : p.entrySet()) {
      String key = (String) e.getKey();
      String value = (String) e.getValue();

      if (!key.startsWith("_")) {
        int currentOrder = Integer.parseInt(value);
        order++;
        assertThat(currentOrder).isEqualTo(order);
      }
    }
  }

  @Test
  public void verifyUTF8() throws IOException {
    Properties p = IO.readUtf8Properties("/play/utils/OrderSaferPropertiesTest.properties");
    verifyPropertiesContent(p);
  }

  @Test
  public void verifyUTF8_via_readUtf8Properties() throws IOException {
    Properties p = IO.readUtf8Properties("/play/utils/OrderSaferPropertiesTest.properties");
    verifyPropertiesContent(p);
  }

  private void verifyPropertiesContent(Properties p) {
    assertThat(p.getProperty("_check_1")).isEqualTo("æøåÆØÅ");
    assertThat(p.getProperty("_check_2")).isEqualTo("equal = % %%'\"");
    assertThat(p.getProperty("_check_3")).isEqualTo("z");
    assertThat(p.getProperty("_check_4")).isEqualTo("\"quoted string\"");
    assertThat(p.getProperty("_check_5")).isEqualTo("newLineString\n");
    assertThat(p.getProperty("_check_6")).isEqualTo("欢迎");
    assertThat(p.getProperty("_check_7.ยินดีต้อนรับ")).isEqualTo("ยินดีต้อนรับ");
    assertThat(p.getProperty("_check_8")).isEqualTo("х");// Unicode Character 'CYRILLIC SMALL LETTER HA' (U+0445)

    String cyrillic_bulgarian_caps = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЬЮЯ";
    String cyrillic_bulgarian_small = "абвгдежзийклмнопрстуфхцчшщъьюя";
    String cyrillic_bulgarian_old = "ѣѢѫѪѭѬ";
    String cyrillic_russian = "ЭЫэы";
    String cyrillic_serbian = "ЉЊЂЋЏљњђћџ";
    assertThat(p.getProperty("_cyrillic_bulgarian_caps")).isEqualTo(cyrillic_bulgarian_caps);
    assertThat(p.getProperty("_cyrillic_bulgarian_small")).isEqualTo(cyrillic_bulgarian_small);
    assertThat(p.getProperty("_cyrillic_bulgarian_old")).isEqualTo(cyrillic_bulgarian_old);
    assertThat(p.getProperty("_cyrillic_russian")).isEqualTo(cyrillic_russian);
    assertThat(p.getProperty("_cyrillic_serbian")).isEqualTo(cyrillic_serbian);
  }

  @SuppressWarnings("SameParameterValue")
  private Properties readJavaProperties(String fileName) throws IOException {
    Properties properties = new Properties();
    properties.load(getClass().getResourceAsStream(fileName));
    return properties;
  }
}
