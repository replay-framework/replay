package ui.hello;

import com.codeborne.pdftest.PDF;
import com.codeborne.selenide.Configuration;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

import static com.codeborne.pdftest.assertj.Assertions.assertThat;

public class HelloPdfWorldSpec extends BaseSpec {
  @Test
  public void downloadHelloWorldPdf() throws IOException {
    PDF pdf = new PDF(new URL(Configuration.baseUrl + "/pdf"));
    assertThat(pdf).containsExactText("Hello, PDF World!");
  }
}