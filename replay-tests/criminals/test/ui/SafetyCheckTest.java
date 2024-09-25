package ui;

import static com.codeborne.selenide.Selenide.open;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.junit.jupiter.api.Test;

public class SafetyCheckTest extends BaseUITest {

  @Test
  public void isSafe_ifHasEmptyHistory() {
    wireMock.stubFor(
        get(urlEqualTo("/criminal-records?ssn=2222222222"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/json")
                    .withBody("[]")));

    DashboardPage page = open("/dashboard", DashboardPage.class);
    CheckResultsPage checkResultsPage = page.checkSsn("2222222222");
    checkResultsPage.verifyNoResults();
  }

  @Test
  public void noSafe_ifCriminalHasHistory() {
    wireMock.stubFor(
        get(urlEqualTo("/criminal-records?ssn=1111111111"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/json")
                    .withBody("[{\"description\":\"убийство\"}, {\"description\":\"грабёж\"}]")));

    DashboardPage page = open("/dashboard", DashboardPage.class);
    CheckResultsPage checkResultsPage = page.checkSsn("1111111111");
    checkResultsPage.verifyResult("Результат: обнаружен криминал. нельзя выпускать на волю.");
  }

  @Test
  public void noSafe_ifFailedToLoadHistory() {
    wireMock.stubFor(
        get(urlEqualTo("/criminal-records?ssn=1111111111"))
            .willReturn(aResponse().withStatus(504)));

    DashboardPage page = open("/dashboard", DashboardPage.class);
    CheckResultsPage checkResultsPage = page.checkSsn("1111111111");
    checkResultsPage.verifyResult(
        "Результат: не удалось проверить историю преступлений. Нельзя выпускать на волю.");
  }
}
