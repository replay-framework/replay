package ui.hello.kotlin

import com.codeborne.selenide.Condition
import com.codeborne.selenide.Selenide
import org.junit.jupiter.api.Test

class TypedInputSpec : BaseSpec() {
  @Test
  fun openPageForced() {
    Selenide.open("/forced/1")
    Selenide.element("h1")
      .shouldHave(Condition.text("Hello, User 1 with identifier type N/A and value type Long!"))
  }

  @Test
  fun openPageGeneric() {
    Selenide.open("/generic/2")
    Selenide.element("h1")
      .shouldHave(Condition.text("Hello, User 2 with identifier type User and value type Long!"))
  }
}