package ui;

import com.codeborne.selenide.Configuration;
import com.github.tomakehurst.wiremock.WireMockServer;
import criminals.Application;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import static com.codeborne.selenide.TextCheck.FULL_TEXT;


public class BaseUITest {
  protected static final WireMockServer wireMock = new WireMockServer(0);
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Before
  public final void startAUT() {
    if (!Play.started) {
      log.info("Starting AUT with classpath {}", System.getProperty("java.class.path"));

      wireMock.start();
      String criminalRecordsServiceUrl = String.format("http://127.0.0.1:%s/criminal-records", wireMock.port());
      int port = new Application().start("test", criminalRecordsServiceUrl);

      Configuration.baseUrl = "http://127.0.0.1:" + port;
      Configuration.browserSize = "1024x800";
      Configuration.textCheck = FULL_TEXT;

      log.info("Started AUT at {}", Configuration.baseUrl);
    }
    else {
      log.info("Running AUT on {}", Configuration.baseUrl);
    }
  }
}
