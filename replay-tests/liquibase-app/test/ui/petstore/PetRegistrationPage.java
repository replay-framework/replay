package ui.petstore;

import com.codeborne.selenide.SelenideElement;
import model.Kind;
import org.openqa.selenium.support.FindBy;

public class PetRegistrationPage {
  @FindBy(name = "pet.kind")
  private SelenideElement kind;

  @FindBy(name = "pet.name")
  private SelenideElement name;

  @FindBy(name = "pet.age")
  private SelenideElement age;

  @FindBy(id = "register")
  private SelenideElement button;

  public void registerPet(Kind kind, String name, int age) {
    this.kind.selectOptionByValue(kind.name());
    this.name.val(name);
    this.age.val(String.valueOf(age));
    this.button.click();
  }
}
