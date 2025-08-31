package ui.hello;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HelloWorldSpec extends BaseSpec {

  @BeforeEach
  final void openPage() {
    open("/");
  }

  @Test
  void openHelloWorldPage() {
    $("h1").shouldHave(text("Hello, world!"));
  }

  @Test
  void linkWithSpace() {
    $("#link-with-space").click();
    $("h1").shouldHave(text("Hello, Tere morning!"));
  }

  @Test
  void linkWithSpaceInPath() {
    $("#link-with-spaces-in-path").click();
    $("h1").shouldHave(text("Hello, The Talented Mr. Ripley!!"));
  }

  @Test
  void linkWithPlus() {
    $("#link-with-plus").click();
    $("h1").shouldHave(text("Hello, You+me!"));
  }

  @Test
  void linkWithPlusInPath() {
    $("#link-with-pluses-in-path").click();
    $("h1").shouldHave(text("Hello, Me+Myself+Irene/2000 (Farrelly brothers)!"));
  }
}
