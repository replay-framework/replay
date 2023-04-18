package ui;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Configuration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;

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
