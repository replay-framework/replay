package play.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.lang.Integer.parseInt;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static int httpPort;

    private EventLoopGroup boss = new NioEventLoopGroup();
    private EventLoopGroup worker = new NioEventLoopGroup(SystemPropertyUtil.getInt("events.workerThreads", 300),
      new DefaultThreadFactory("nio-worker", Thread.MAX_PRIORITY));

    public Server() {
        this(parseInt(Play.configuration.getProperty("http.port", "9000")));
    }

    public Server(int port) {
        httpPort = port;
    }

    public void start() throws InterruptedException {
        System.setProperty("file.encoding", "utf-8");
        InetAddress address = address();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(boss, worker)
              .channel(NioServerSocketChannel.class)
              .childHandler(new HttpServerPipelineFactory());

            setChannelOptions(bootstrap);

            Channel ch = bootstrap.bind(httpPort).sync().channel();

            logger.info(">> startUp server [{}]", ch.localAddress().toString());

            ch.closeFuture().sync(); // blocked

        } finally {
            shutdown();
        }

        if (Play.mode == Play.Mode.DEV) {
            if (address == null) {
                logger.info("Listening for HTTP on port {} (Waiting a first request to start) ...", httpPort);
            } else {
                logger.info("Listening for HTTP at {}:{} (Waiting a first request to start) ...", address, httpPort);
            }
        } else {
            if (address == null) {
                logger.info("Listening for HTTP on port {} ...", httpPort);
            } else {
                logger.info("Listening for HTTP at {}:{}  ...", address, httpPort);
            }
        }
    }

    protected void shutdown() {
        boss.shutdownGracefully();
        worker.shutdownGracefully();
    }

    protected void setChannelOptions(ServerBootstrap bootstrap) {
        bootstrap.childOption(ChannelOption.MAX_MESSAGES_PER_READ, 36);
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
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("Cannot resolve address " + host, e);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Play play = new Play();
        play.init(System.getProperty("play.id", ""));

        if (Play.mode.isDev()) {
            new Server().start();
            play.start();
        }
        else {
            play.start();
            new Server().start();
        }

        logger.info("~ Server is up and running on port " + httpPort);
    }
}
