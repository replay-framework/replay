package ui.hello;

import com.codeborne.selenide.Configuration;
import org.junit.After;
import org.junit.Before;
import play.Play;
import play.server.Server;

import java.io.File;

import static org.openqa.selenium.net.PortProber.findFreePort;

public class BaseSpec {
  Play play = new Play();

  @Before
  public void setUp() throws InterruptedException {
    Thread playStarter = new Thread(() -> {
      play.init(new File(System.getProperty("application.path", ".")), "test");
      play.start();

      int port = findFreePort();
      new Server(new String[]{"--http.port=" + port});

      Configuration.baseUrl = "http://localhost:" + port;
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
    }, "Play! starter thread");

    playStarter.start();
    playStarter.join();

    Configuration.browser = "chrome";
    Configuration.headless = true;
  }

  @After
  public void tearDown() {
    play.stop();
  }
}
