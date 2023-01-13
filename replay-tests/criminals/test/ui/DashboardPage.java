package ui;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.page;

public class DashboardPage {
  final SelenideElement header = $("h1");
  private final SelenideElement ssn = $(By.name("ssn"));

  public CheckResultsPage checkSsn(String ssn) {
    this.ssn.val(ssn).pressEnter();
    return page(CheckResultsPage.class);
  }
}
