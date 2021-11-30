package ui.hello

import com.codeborne.pdftest.PDF
import com.codeborne.pdftest.assertj.Assertions.assertThat
import com.codeborne.selenide.Configuration
import org.junit.Test
import java.net.URL

class HelloPdfWorldSpec : BaseSpec() {
  @Test
  fun downloadHelloWorldPdf() {
    val pdf = PDF(URL(Configuration.baseUrl + "/pdf"))
    assertThat(pdf).containsExactText("Hello, PDF World on Kotlin!")
  }
}