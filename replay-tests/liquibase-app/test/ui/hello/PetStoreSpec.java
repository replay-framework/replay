package ui.hello;

import org.junit.Test;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

public class PetStoreSpec extends BaseSpec {
  @Test
  public void selectsDataFromDB() {
    open("/");
    $("h1").shouldHave(text("Hello, Pet Admin!"));
  }
}