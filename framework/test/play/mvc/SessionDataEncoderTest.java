package play.mvc;

import org.junit.Test;
import play.libs.Codec;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SessionDataEncoderTest {
  SessionDataEncoder encoder = new SessionDataEncoder();

  @Test
  public void testDecode() {
    String data = "___ID=d3ac6dc3-479b-4ea0-b246-5c88c10aeaa8&username=Андрей%26Co&__IBTS=1409147543161&___AT=b19de04a9bd277c928cb3c27f0ce90db5aaca215";
    Map<String, String> map = encoder.decode(Codec.encodeBASE64(data));
    assertThat(map.get("___ID")).isEqualTo("d3ac6dc3-479b-4ea0-b246-5c88c10aeaa8");
    assertThat(map.get("username")).isEqualTo("Андрей&Co");
    assertThat(map.get("__IBTS")).isEqualTo("1409147543161");
    assertThat(map.get("___AT")).isEqualTo("b19de04a9bd277c928cb3c27f0ce90db5aaca215");
  }

  @Test
  public void testEncode() {
    Map<String, String> map = new LinkedHashMap<>();
    map.put("username", "Андрей&Co%50=x+y");
    assertThat(encoder.encode(map)).isEqualTo(Codec.encodeBASE64("username=Андрей%26Co%2550%3Dx%2By"));
  }
}