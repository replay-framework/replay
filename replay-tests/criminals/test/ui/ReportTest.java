package ui;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Configuration;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;

public class ReportTest extends BaseUITest {
  @Test
  public void downloadReportAsPDF() throws IOException {
    PDF pdf = new PDF(new URL(Configuration.baseUrl + "/report/pdf?days=42"));
    assertThat(pdf).containsExactText("Hello, Anonymous!");
    assertThat(pdf).containsExactText("This is the report for last 42 days");
  }
}
