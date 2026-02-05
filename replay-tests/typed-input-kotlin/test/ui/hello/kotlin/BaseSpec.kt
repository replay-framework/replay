package ui.hello.kotlin

import com.codeborne.selenide.Configuration
import org.junit.jupiter.api.BeforeEach
import play.Play
import play.server.Starter

open class BaseSpec {
  @BeforeEach
  fun setUp() {
    val port = Starter.start("test")
    Configuration.baseUrl = "http://localhost:$port"
    Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl)
    System.setProperty("webdriver.http.factory", "jdk-http-client")
  }
}
