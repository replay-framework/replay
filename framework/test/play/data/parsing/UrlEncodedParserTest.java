package play.data.parsing;

import org.junit.jupiter.api.Test;
import play.mvc.Http;

import java.io.*;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class UrlEncodedParserTest {
  private final UrlEncodedParser parser = new UrlEncodedParser();

  @Test
  public void parsePlainQuerystring() {
    Http.Request request = givenRequest("name=Jóhn Doe&city=New Yørk");
    parser.forQueryString = true;
    Map<String, String[]> params = parser.parse(new ByteArrayInputStream(request.querystring.getBytes(UTF_8)), UTF_8);
    assertThat(params.get("name")[0]).isEqualTo("Jóhn Doe");
    assertThat(params.get("city")[0]).isEqualTo("New Yørk");
  }

  @Test
  public void parseEncodedQuerystring() {
    Http.Request request = givenRequest("name=J%C3%B3hn%20Doe&city=New%20Y%C3%B8rk");
    parser.forQueryString = true;
    Map<String, String[]> params = parser.parse(new ByteArrayInputStream(request.querystring.getBytes(UTF_8)), UTF_8);
    assertThat(params.get("name")[0]).isEqualTo("Jóhn Doe");
    assertThat(params.get("city")[0]).isEqualTo("New Yørk");
  }

  @Test
  public void parseUtf16EncodedQuerystring() {
    Http.Request request = givenRequest("name=%FE%FF%51%B0%6D%C7%6D%CB");
    parser.forQueryString = true;
    Map<String, String[]> params = parser.parse(new ByteArrayInputStream(request.querystring.getBytes(UTF_16)), UTF_16);
    assertThat(params.get("name")[0]).isEqualTo("冰淇淋");
  }

  private Http.Request givenRequest(String querystring) {
    Http.Request request = new Http.Request();
    request.querystring = querystring;
    request.encoding = UTF_8;
    return request;
  }
}
