package ui.petstore;

import app.LiquiBaseApp;
import com.codeborne.selenide.Configuration;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import java.util.concurrent.atomic.AtomicBoolean;

public class BaseSpec {
  private static final Logger log = LoggerFactory.getLogger(BaseSpec.class);
  private static final AtomicBoolean started = new AtomicBoolean();

  @Before
  public synchronized void setUp() throws Exception {
    if (!started.get()) {
      int port = LiquiBaseApp.run("test");
      Configuration.baseUrl = "http://localhost:" + port;
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
      started.set(true);
    }
  }
}
