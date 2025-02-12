package ui.hello;

import static com.codeborne.selenide.Condition.href;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.webdriver;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LinksInHtmlSpec extends BaseSpec {

  @BeforeEach
  final void openHelloPage() {
    open("/");
  }

  @Test
  public void linkToController() {
    $("h1").shouldHave(text("Hello, world!"));

    $("#repeat").shouldHave(text("Repeat 666 times"));
    $("#repeat").click();

    $("h1").shouldHave(text("Hello, world #666!"));
  }

  @Test
  public void linkToStaticFile() {
    $("#link-to-static-file")
        .shouldHave(href("/public/img/banksy-kyiv-7.jpeg"))
        .click();
    webdriver().shouldHave(urlContaining("/public/img/banksy-kyiv-7.jpeg"));
  }
}
