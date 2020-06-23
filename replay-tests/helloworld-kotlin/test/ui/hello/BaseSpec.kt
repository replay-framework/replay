package ui.hello

import com.codeborne.selenide.Configuration
import org.junit.Before
import org.openqa.selenium.net.PortProber.findFreePort
import play.Play
import play.server.Server

open class BaseSpec {
  private val play = Play()

  @Before
  fun setUp() {
    val playStarter = Thread({
      play.init("test")
      play.start()
      val port = findFreePort()
      Server(play, port).start()
      Configuration.baseUrl = "http://localhost:$port"
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl)
    }, "Play! starter thread")
    playStarter.start()
    playStarter.join()
    Configuration.browser = "chrome"
    Configuration.headless = true
  }
}