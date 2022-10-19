package ui.hello;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.download;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.assertj.core.api.Assertions.assertThat;


public class HelloWorldSpec extends BaseSpec {
  @Test
  public void openHelloWorldPage() {
    open("/");
    $("h1").shouldHave(text("Hello, world!"));
  }

  @Test
  public void openStaticFile() throws IOException, URISyntaxException {
    File downloadedFile = download("/public/hello_world.txt", 4000);

    assertThat(downloadedFile.getName())
            .isEqualTo("hello_world.txt");
    assertThat(readFileToString(downloadedFile, "UTF-8"))
            .isEqualTo("Hello, WinRar!");
  }
}
