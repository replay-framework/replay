package ui.hello;

import com.codeborne.selenide.Configuration;
import io.restassured.RestAssured;
import org.junit.Before;
import play.Play;
import play.server.Server;

import java.util.concurrent.atomic.AtomicBoolean;

public class BaseSpec {
  private static final AtomicBoolean appStarted = new AtomicBoolean(false);
  private static final Play play = new Play();

  @Before
  public void setUp() {
    System.setProperty("webdriver.http.factory", "jdk-http-client");
    if (appStarted.get()) return;
    startApp();
  }

  private static synchronized void startApp() {
    if (appStarted.get()) return;

    play.init("test");
    play.start();
    int port = new Server(play).start();

    Configuration.baseUrl = "http://localhost:" + port;
    Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);

    RestAssured.baseURI = Configuration.baseUrl;
    RestAssured.port = port;
    appStarted.set(true);
  }
}
