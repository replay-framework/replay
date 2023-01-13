import play.Play;
import play.server.Server;

public class HelloWorldApp {
  public static void main(String[] args) {
    Play play = new Play();
    play.init("prod");
    play.start();
    new Server(play).start();

    System.out.println("Try: http://localhost:" + Server.httpPort);
    System.out.println("Try: http://localhost:" + Server.httpPort + "/public/hello_world.txt");
    System.out.println();
  }
}
