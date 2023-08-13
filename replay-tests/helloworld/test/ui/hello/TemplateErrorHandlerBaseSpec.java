package ui.hello;

import com.codeborne.selenide.Configuration;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import play.Play;
import main.HelloWorldApp;

import java.util.concurrent.atomic.AtomicBoolean;

public class TemplateErrorHandlerBaseSpec {
  private static final AtomicBoolean appStarted = new AtomicBoolean(false);

  @BeforeEach
  public void setUp() {
    System.setProperty("webdriver.http.factory", "jdk-http-client");
    if (appStarted.get()) return;
    startApp();
  }

  private static synchronized void startApp() {
    if (appStarted.get()) return;

    int port = HelloWorldApp.startWithTemplateErrorHandler("test");

    Configuration.baseUrl = "http://localhost:" + port;
    Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);

    RestAssured.baseURI = Configuration.baseUrl;
    RestAssured.port = port;
    appStarted.set(true);
  }
}
