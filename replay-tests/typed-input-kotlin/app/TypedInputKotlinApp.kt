import play.server.Starter

object TypedInputKotlinApp {
  @JvmStatic
  fun main(args: Array<String>) {
    val port = Starter.start("prod")
    println("Try: http://localhost:$port/<forced|generic>/{long}")
    println()
  }
}