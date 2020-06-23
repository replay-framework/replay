package ui.hello

import com.codeborne.selenide.Condition.text
import com.codeborne.selenide.Selenide.element
import com.codeborne.selenide.Selenide.open
import org.junit.Test

class HelloWorldSpec : BaseSpec() {
  @Test
  fun openHelloWorldPage() {
    open("/")
    element("h1").shouldHave(text("Hello, world!"))
  }
}