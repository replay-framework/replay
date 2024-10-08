package ui.hello;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Configuration;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class HelloPdfWorldSpec extends BaseSpec {
  @Test
  public void downloadHelloWorldPdf() throws IOException {
    PDF pdf = new PDF(new URL(Configuration.baseUrl + "/pdf"));
    assertThat(pdf).containsExactText("Hello, PDF World!");
    assertThat(pdf).containsExactText("Choose your role");
    assertThat(pdf).containsExactText("Choose your car");
  }

  @Test
  public void downloadHelloWorldPdfGeneratedFromPlainText() throws IOException {
    PDF pdf = new PDF(new URL(Configuration.baseUrl + "/pdf/text"));
    assertThat(pdf).containsExactText("Hello, PDF from plain text! Sincerely yours, RePlay");
  }

  @Test
  public void downloadHelloWorldPdf_requestFormat() throws IOException {
    PDF pdf = new PDF(new URL(Configuration.baseUrl + "/pdf/text2"));
    assertThat(pdf).containsExactText("Hello, Request format! Sincerely yours, RePlay");
  }
}
