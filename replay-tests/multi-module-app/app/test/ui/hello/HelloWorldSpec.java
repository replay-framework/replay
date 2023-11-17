package ui.hello;

import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class HelloWorldSpec extends BaseSpec {
  @Test
  public void openHelloWorldPage() {
    open("/");
    $("h1").shouldHave(text("Hello, world!"));
  }
}
