package ui.hello;

import com.codeborne.selenide.Configuration;
import com.google.common.base.Strings;
import com.google.gson.JsonParser;
import org.junit.Test;
import play.libs.ws.HttpResponse;
import play.libs.ws.WSAsync;

import static org.assertj.core.api.Assertions.assertThat;

public class LargePostBodySpec extends BaseSpec {

  @Test
  public void exerciseFileChannelBufferWithLargePostBody() {
    final int CONTENT_LENGTH = 1024 * 9;

    HttpResponse res = (new WSAsync()).newRequest(Configuration.baseUrl + "/post")
        .mimeType("application/json")
        // large bodies become a ResettableFileInputStream
        .body(Strings.repeat("X", CONTENT_LENGTH))
        .post();

    // These do not test the warning from TextParser.resetBodyInputStreamIfPossible
    // is actually written to the logs. We do test the request came through with the correct length.

    assertThat(res.getStatus()).isEqualTo(200);

    final int contentLengthFormBody =
        JsonParser.parseString(res.getString()).getAsJsonObject().get("content-length").getAsInt();
    assertThat(CONTENT_LENGTH).isEqualTo(contentLengthFormBody);
  }
}
