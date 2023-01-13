package ui;

import com.codeborne.selenide.SelenideElement;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

public class CheckResultsPage {

  private final SelenideElement results = $("#result");

  public void verifyNoResults() {
    verifyResult("Результат: криминальная история чиста. можно выпускать на волю.");
  }

  public void verifyResult(String expectedResult) {
    results.shouldHave(text(expectedResult));
  }
}
