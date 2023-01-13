package ui.hello;

import com.codeborne.selenide.Configuration;
import org.junit.Before;
import play.Play;
import play.server.Server;

public class BaseSpec {
  Play play = new Play();

  @Before
  public void setUp() throws InterruptedException {
    Thread playStarter = new Thread(() -> {
      play.init("test");
      play.start();
      int port = new Server(play).start();

      Configuration.baseUrl = "http://localhost:" + port;
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
    }, "Play! starter thread");

    playStarter.start();
    playStarter.join();
  }
}
