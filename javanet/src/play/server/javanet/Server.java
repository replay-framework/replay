package play.server.javanet;

import static java.lang.Integer.parseInt;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import javax.annotation.ParametersAreNonnullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.Play.Mode;

@ParametersAreNonnullByDefault
public class Server {
  private static final Logger logger = LoggerFactory.getLogger(Server.class);

  @Deprecated public static int httpPort;
  private final Play play;
  private int port;

  public Server(Play play) {
    this(play, parseInt(Play.configuration.getProperty("http.port", "9000")));
  }

  public Server(Play play, int port) {
    this.play = play;
    this.port = port;
    httpPort = port;
  }

  /*
   * Inspired by https://dzone.com/articles/simple-http-server-in-java
   */
  public int start() {
    System.setProperty("file.encoding", "utf-8");

    String address = address();
    try {
      HttpServer server = HttpServer.create(new InetSocketAddress(address, port), 0);
      ThreadPoolExecutor threadPoolExecutor =
          (ThreadPoolExecutor) Executors.newFixedThreadPool(10); // TODO

      server.createContext("/", new PlayHandler(Play.invoker, play.getActionInvoker()));
      server.setExecutor(threadPoolExecutor);
      server.start();
      readActualPort(server);

      String modeSuffix = Play.mode == Mode.DEV ? " (Waiting a first request to start)" : "";
      logger.info("Listening for HTTP at http://{}:{}{} ...", address, port, modeSuffix);
      return port;
    } catch (IOException e) {
      throw new RuntimeException("Failed to start server on " + address + ':' + port, e);
    }
  }

  private void readActualPort(HttpServer server) {
    if (port == 0) {
      this.port = server.getAddress().getPort();
      httpPort = server.getAddress().getPort();
      Play.configuration.setProperty("http.port", String.valueOf(port));
    }
  }

  private String address() {
    if (Play.configuration.getProperty("http.address") != null) {
      return Play.configuration.getProperty("http.address");
    }

    if (System.getProperties().containsKey("http.address")) {
      return System.getProperty("http.address");
    }

    return "0.0.0.0";
  }

  public int port() {
    return port;
  }
}
