package ui.hello;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.download;
import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class HelloWorldSpec extends BaseSpec {
  @Test
  public void openHelloWorldPage() {
    open("/");
    $("h1").shouldHave(text("Hello, world!"));
  }

  @Test
  public void openFavicon() throws IOException, URISyntaxException {
    File downloadedFile = download("/img/favicon.png", 4000);

    assertThat(downloadedFile.getName()).isEqualTo("favicon.png");
    assertThat(downloadedFile).hasDigest("MD5", "abd0852cbbbda55586f19e9aebb15d06");
  }
}
