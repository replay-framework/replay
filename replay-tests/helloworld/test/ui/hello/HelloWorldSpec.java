package ui.hello;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static com.codeborne.selenide.Condition.image;
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
    $("#img1").shouldBe(image);
    $("#img2").shouldBe(image);
    $("#img3").shouldBe(image);
    $("#img4").shouldNotBe(image);
    $("#img5").shouldBe(image);
    $("#img6").shouldBe(image);
    $("#img7").shouldBe(image);
  }

  @Test
  public void openStaticFile() throws IOException, URISyntaxException {
    File downloadedFile = download("/public/hello_world.txt", 4000);

    assertThat(downloadedFile.getName())
            .isEqualTo("hello_world.txt");
    assertThat(readFileToString(downloadedFile, "UTF-8"))
            .isEqualTo("Hello, WinRar!");
  }

  @Test
  public void openImage() throws IOException, URISyntaxException {
    File downloadedFile = download("/img/favicon.png", 4000);

    assertThat(downloadedFile.getName()).isEqualTo("favicon.png");
    assertThat(downloadedFile).content().hasSize(707);
  }
}
