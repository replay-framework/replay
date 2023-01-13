package ui;

import com.codeborne.selenide.Configuration;
import com.github.tomakehurst.wiremock.WireMockServer;
import criminals.Application;
import org.junit.Before;
import play.Play;

import static com.codeborne.selenide.TextCheck.FULL_TEXT;


public class BaseUITest {
  protected static final WireMockServer wireMock = new WireMockServer(0);

  @Before
  public final void startAUT() {
    if (!Play.started) {
      wireMock.start();
      String criminalRecordsServiceUrl = String.format("http://0.0.0.0:%s/criminal-records", wireMock.port());
      int port = new Application().start("test", criminalRecordsServiceUrl);

      Configuration.baseUrl = "http://0.0.0.0:" + port;
      Configuration.browserSize = "1024x800";
      Configuration.browser = "chrome";
      Configuration.textCheck = FULL_TEXT;
    }
  }
}
