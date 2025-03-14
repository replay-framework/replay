package ui.hello;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.download;
import static com.codeborne.selenide.Selenide.open;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

import jobs.AppJob;
import jobs.CoreJob;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import play.Play;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class HelloWorldSpec extends BaseSpec {
  @Test
  public void openFavicon() throws IOException, URISyntaxException {
    File downloadedFile = download("/img/favicon.png", 4000);

    assertThat(downloadedFile.getName()).isEqualTo("favicon.png");
    assertThat(downloadedFile).hasDigest("MD5", "abd0852cbbbda55586f19e9aebb15d06");
  }

  @Test
  @Disabled("Locally runs fine, but with GitHub workflow it causes java.net.SocketTimeoutException: Read timed out")
  public void saveHelloWorldPageAndCheckMeta() throws URISyntaxException, IOException {
    File helloFile = download("/", 4000);

    assertThat(helloFile).content().containsIgnoringWhitespaces(IOUtils.toString(
        requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream("views/meta.html")), UTF_8));
  }

  @Test
  public void openHelloWorldPageAndCheckInclude() {
    open("/");
    $("h2#ic").shouldHave(text("included from core"));
  }

  @Test
  public void openStatusPage() throws URISyntaxException {
    File statusFile = download("/status.txt", 4000);

    assertThat(statusFile).content().contains(AppJob.class.getName() + " run at application start. (last run at");
    assertThat(statusFile).content().contains(CoreJob.class.getName() + " run at application start. (last run at");
  }

  @Test
  public void checkIncludedConfValueFromLocal() {
    assertThat(Play.configuration.getProperty("application.app.included")).isEqualTo("app.conf included!");
  }

  @Test
  public void checkIncludedConfValueFromClasspath() {
    assertThat(Play.configuration.getProperty("application.core.included")).isEqualTo("core.conf included!");
  }
}
