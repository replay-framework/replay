package ui.hello;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import org.junit.jupiter.api.Test;

public class HelloWorldSpec extends BaseSpec {
  @Test
  public void openHelloWorldPage() {
    open("/");
    $("h1").shouldHave(text("Hello, world!"));
  }

  @Test
  public void linkWithSpace() {
    open("/");
    $("h1").shouldHave(text("Hello, world!"));
    $("#link-with-space").click();
    $("h1").shouldHave(text("Hello, Tere morning!"));
  }

  @Test
  public void linkWithPlus() {
    open("/");
    $("h1").shouldHave(text("Hello, world!"));
    $("#link-with-plus").click();
    $("h1").shouldHave(text("Hello, You+me!"));
  }
}
