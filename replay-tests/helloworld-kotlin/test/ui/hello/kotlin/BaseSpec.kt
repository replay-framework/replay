package ui.hello.kotlin

import com.codeborne.selenide.Configuration
import org.junit.Before
import org.openqa.selenium.net.PortProber
import play.Play
import play.server.Server

open class BaseSpec {
  private val play = Play()

  @Before
  fun setUp() {
    val playStarter = Thread({
      play.init("test")
      play.start()
      val port = Server(play).start()
      Configuration.baseUrl = "http://localhost:$port"
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl)
    }, "Play! starter thread")
    playStarter.start()
    playStarter.join()
  }
}