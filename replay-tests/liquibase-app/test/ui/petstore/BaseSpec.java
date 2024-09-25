package ui.petstore;

import static java.net.http.HttpRequest.BodyPublishers.noBody;
import static java.nio.charset.StandardCharsets.UTF_8;

import app.LiquiBaseApp;
import com.codeborne.selenide.Configuration;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

public class BaseSpec {
  private static final Logger log = LoggerFactory.getLogger(BaseSpec.class);
  private static final AtomicBoolean started = new AtomicBoolean();
  private static final HttpClient http = HttpClient.newBuilder().build();

  @BeforeEach
  public synchronized void setUp() throws Exception {
    System.setProperty("webdriver.http.factory", "jdk-http-client");

    if (!started.get()) {
      int port = LiquiBaseApp.run("test");
      Configuration.baseUrl = "http://localhost:" + port;
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
      started.set(true);
    }
    deleteAllPets();
  }

  private void deleteAllPets() throws URISyntaxException, IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .method("DELETE", noBody())
            .uri(new URI(Configuration.baseUrl + "/pet/all"))
            .build();
    HttpResponse<String> response = http.send(request, BodyHandlers.ofString(UTF_8));
    log.info("Reset {} pets", response.body());
  }
}
