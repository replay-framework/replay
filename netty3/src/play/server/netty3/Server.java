package play.server.netty3;

import static java.lang.Integer.parseInt;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.Play.Mode;

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

  public int start() {
    System.setProperty("file.encoding", "utf-8");

    ServerBootstrap bootstrap =
        new ServerBootstrap(
            new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
    InetAddress address = address();
    bootstrap.setPipelineFactory(
        new HttpServerPipelineFactory(Play.invoker, play.getActionInvoker()));
    Channel ch = bootstrap.bind(new InetSocketAddress(address, port));
    bootstrap.setOption("child.tcpNoDelay", true);
    readActualPort(ch);

    String modeSuffix = Play.mode == Mode.DEV ? " (Waiting a first request to start)" : "";
    String hostname = address == null ? "0.0.0.0" : address.getHostName();
    logger.info("Listening for HTTP at {}:{}{} ...", hostname, port, modeSuffix);
    return port;
  }

  private void readActualPort(Channel ch) {
    if (port == 0) {
      InetSocketAddress socketAddress = (InetSocketAddress) ch.getLocalAddress();
      this.port = socketAddress.getPort();
      httpPort = socketAddress.getPort();
      Play.configuration.setProperty("http.port", String.valueOf(port));
    }
  }

  private InetAddress address() {
    if (Play.configuration.getProperty("http.address") != null) {
      return address(Play.configuration.getProperty("http.address"));
    }

    if (System.getProperties().containsKey("http.address")) {
      return address(System.getProperty("http.address"));
    }

    return null;
  }

  private InetAddress address(String host) {
    try {
      return InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Cannot resolve address " + host, e);
    }
  }

  public int port() {
    return port;
  }
}
