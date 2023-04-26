package ui.hello.kotlin

import com.codeborne.selenide.Configuration
import org.junit.Before
import play.Play
import play.server.Server

open class BaseSpec {
  private val play = Play()

  @Before
  fun setUp() {
    play.init("test")
    play.start()
    val port = Server(play).start()
    Configuration.baseUrl = "http://localhost:$port"
    Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl)
    System.setProperty("webdriver.http.factory", "jdk-http-client")
  }
}