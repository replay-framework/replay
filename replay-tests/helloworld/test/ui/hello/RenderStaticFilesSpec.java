package ui.hello;

import static com.codeborne.selenide.Condition.image;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.download;
import static com.codeborne.selenide.Selenide.open;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;

public class RenderStaticFilesSpec extends BaseSpec {
  @Test
  public void loadsBigImages() {
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

    assertThat(downloadedFile.getName()).isEqualTo("hello_world.txt");
    assertThat(readFileToString(downloadedFile, "UTF-8")).isEqualTo("Hello, WinRar!");
  }

  @Test
  public void openImage() throws IOException, URISyntaxException {
    File downloadedFile = download("/img/favicon.png", 4000);

    assertThat(downloadedFile.getName()).isEqualTo("favicon.png");
    assertThat(downloadedFile)
        .binaryContent()
        .isEqualTo(toByteArray(requireNonNull(getClass().getResourceAsStream("favicon.png"))));
  }
}
