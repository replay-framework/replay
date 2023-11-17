package ui.hello;

import com.codeborne.selenide.Configuration;
import org.junit.jupiter.api.BeforeEach;
import play.Play;
import play.server.Starter;

import java.util.concurrent.atomic.AtomicBoolean;

public class BaseSpec {
  private static final AtomicBoolean appStarted = new AtomicBoolean(false);

  @BeforeEach
  public void setUp() {
    System.setProperty("webdriver.http.factory", "jdk-http-client");
    if (appStarted.get()) return;
    startApp();
  }

  private static synchronized void startApp() {
    if (appStarted.get()) return;

    int port = Starter.start("test");

    Configuration.baseUrl = "http://localhost:" + port;
    Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);

    appStarted.set(true);
  }
}
