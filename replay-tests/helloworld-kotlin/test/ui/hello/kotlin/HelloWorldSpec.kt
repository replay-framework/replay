package ui.hello.kotlin

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide
import org.junit.Test

class HelloWorldSpec : BaseSpec() {
  @Test
  fun openHelloWorldPage() {
    Selenide.open("/")
    Selenide.element("h1").shouldHave(Condition.text("Hello, world!"))
  }
}