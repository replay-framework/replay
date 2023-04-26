package ui.hello.kotlin

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.readFileToString
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.net.URISyntaxException

class HelloWorldSpec : BaseSpec() {
  @Test
  fun openHelloWorldPage() {
    Selenide.open("/")
    Selenide.element("h1").shouldHave(Condition.text("Hello, world!"))
  }

  @Test
  fun openStaticFile() {
    val downloadedFile = Selenide.download("/public/hello_world.txt", 4000)
    assertThat(downloadedFile.name).isEqualTo("hello_world.txt")
    assertThat(readFileToString(downloadedFile, "UTF-8")).isEqualTo("Hello, WinRar!")
  }
}