package ui.petstore;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.Selenide.page;

import model.Kind;
import org.junit.jupiter.api.Test;

public class PetsRegistrationSpec extends BaseSpec {

  @Test
  public void canRegisterNewPet() {
    open("/");
    $$("#pets .pet").shouldHave(size(0));
    $("#buttonRegisterPet").shouldHave(text("Register new pet")).click();

    PetRegistrationPage page = page();
    page.registerPet(Kind.COW, "Muuuuusie", 2);

    $$("#pets .pet").shouldHave(size(1));
    $("#totalCount").shouldHave(text("Total count: 1"));
  }
}
