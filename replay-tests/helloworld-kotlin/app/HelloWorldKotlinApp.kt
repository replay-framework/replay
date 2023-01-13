import play.Play
import play.server.Server

object HelloWorldKotlinApp {
  @JvmStatic
  fun main(args: Array<String>) {
    val play = Play()
    play.init("prod")
    play.start()
    Server(play).start()
    println("Try: http://localhost:" + Server.httpPort)
    println("Try: http://localhost:" + Server.httpPort + "/public/hello_world.txt")
    println()
  }
}