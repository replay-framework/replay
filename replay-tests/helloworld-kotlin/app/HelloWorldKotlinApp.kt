import play.server.Starter

object HelloWorldKotlinApp {
  @JvmStatic
  fun main(args: Array<String>) {
    val port = Starter.start("prod")
    println("Try: http://localhost:$port")
    println("Try: http://localhost:$port/public/hello_world.txt")
    println()
  }
}