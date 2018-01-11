package play.server;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.Play.Mode;
import play.libs.IO;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Properties;
import java.util.concurrent.Executors;

public class Server {
    private static final Logger logger = LoggerFactory.getLogger(Server.class);

    public static int httpPort;

    private static final String PID_FILE = "server.pid";

    public Server(String[] args) {

        System.setProperty("file.encoding", "utf-8");
        Properties p = Play.configuration;

        httpPort = Integer.parseInt(getOpt(args, "http.port", p.getProperty("http.port", "9000")));

        InetAddress address = null;

        try {
            if (p.getProperty("http.address") != null) {
                address = InetAddress.getByName(p.getProperty("http.address"));
            } else if (System.getProperties().containsKey("http.address")) {
                address = InetAddress.getByName(System.getProperty("http.address"));
            }

        } catch (Exception e) {
            logger.error("Could not understand http.address", e);
            Play.fatalServerErrorOccurred();
        }

        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(), Executors.newCachedThreadPool())
        );
        try {
            bootstrap.setPipelineFactory(new HttpServerPipelineFactory());

            bootstrap.bind(new InetSocketAddress(address, httpPort));
            bootstrap.setOption("child.tcpNoDelay", true);

            if (Play.mode == Mode.DEV) {
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

        } catch (ChannelException e) {
            logger.error("Could not bind on port {}", httpPort, e);
            Play.fatalServerErrorOccurred();
        }

        if (Play.mode == Mode.DEV || Play.runningInTestMode()) {
           // print this line to STDOUT - not using logger, so auto test runner will not block if logger is misconfigured (see #1222)
           System.out.println("~ Server is up and running");
        }
    }

    private String getOpt(String[] args, String arg, String defaultValue) {
        String s = "--" + arg + "=";
        for (String a : args) {
            if (a.startsWith(s)) {
                return a.substring(s.length());
            }
        }
        return defaultValue; 
    }

    private static void writePID(File root) {
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        File pidfile = new File(root, PID_FILE);
        if (pidfile.exists()) {
            throw new RuntimeException("The " + PID_FILE + " already exists. Is the server already running?");
        }
        IO.write(pid.getBytes(), pidfile);
    }

    public static void main(String[] args) {
        File root = new File(System.getProperty("user.dir"));
        if (System.getProperty("precompiled", "false").equals("true")) {
            Play.usePrecompiled = true;
        }
        if (System.getProperty("writepid", "false").equals("true")) {
            writePID(root);
        }

        Play play = new Play();
        play.init(root, System.getProperty("play.id", ""));

        // TODO Remove support for "precompile"
        if (System.getProperty("precompile") != null) {
            logger.info("precompile done.");
            return;
        }

        if (Play.mode.isDev()) {
            new Server(args);
            play.start();
        }
        else {
            play.start();
            new Server(args);
        }
    }
}
