package ui.hello;

import com.codeborne.selenide.Configuration;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import play.libs.ws.HttpResponse;
import play.libs.ws.WSAsync;

import static org.assertj.core.api.Assertions.assertThat;

public class LargePostBodySpec extends BaseSpec {

  @Test
  public void exerciseFileChannelBufferWithLargePostBody() {
    int contentLength = 1024 * 1024;
    String requestBody = Strings.repeat("X", contentLength);

    HttpResponse res = (new WSAsync()).newRequest(Configuration.baseUrl + "/post")
        .mimeType("application/json")
        // large bodies become a ResettableFileInputStream
        .body(requestBody)
        .post();

    assertThat(res.getStatus()).isEqualTo(200);

    JsonObject responseJson = JsonParser.parseString(res.getString()).getAsJsonObject();
    assertThat(responseJson.get("content-length").getAsInt()).isEqualTo(contentLength);
    assertThat(responseJson.get("origin").getAsString()).isEqualTo(requestBody);
  }
}
