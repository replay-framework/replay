package ui;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Configuration;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportTest extends BaseUITest {
  private static final Logger log = LoggerFactory.getLogger(ReportTest.class);

  @Test
  public void downloadReportAsPDF() throws IOException {
    URL url = new URL(Configuration.baseUrl + "/report/pdf?days=42");
    log.info("Verifying PDF {} ...", url);
    PDF pdf = new PDF(url);
    assertThat(pdf).containsExactText("Hello, Anonymous!");
    assertThat(pdf).containsExactText("This is the report for last 42 days");
  }
}
