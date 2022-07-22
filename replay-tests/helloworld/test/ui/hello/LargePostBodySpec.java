package ui.hello;

import static org.assertj.core.api.Assertions.assertThat;

import com.codeborne.selenide.Configuration;
import com.google.common.base.Strings;
import org.junit.Test;
import play.libs.ws.HttpResponse;
import play.libs.ws.WSAsync;

public class LargePostBodySpec extends BaseSpec {

  @Test
  public void exerciseFileChannelBufferWithLargePostBody() {

    HttpResponse res = (new WSAsync()).newRequest(Configuration.baseUrl + "/post")
        .mimeType("application/json")
        // large bodies become a ResettableFileInputStream
        .body(Strings.repeat("X", 1024*9))
        .post();

    // This does not test the warning is actually written to the logs.
    // Not sure how to do that, nor if that's actually what we are looking for.
    assertThat(res.getStatus()).isEqualTo(200);
  }
}
