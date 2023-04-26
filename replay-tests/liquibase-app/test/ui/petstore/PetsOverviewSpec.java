package ui.petstore;

import org.junit.jupiter.api.Test;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

public class PetsOverviewSpec extends BaseSpec {
  @Test
  public void showsAllRegisteredPets() {
    open("/");
    $("h1").shouldHave(text("Hello, Pet Admin!"));
    $$("#pets .pet").shouldHave(size(0));
    $("#totalCount").shouldHave(text("Total count: 0"));
  }
}