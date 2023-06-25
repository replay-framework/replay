import play.server.Starter;

public class HelloWorldApp {
  public static void main(String[] args) {
    int port = Starter.start("prod");

    System.out.println("Try: http://localhost:" + port);
    System.out.println("Try: http://localhost:" + port + "/public/hello_world.txt");
    System.out.println();
  }
}
