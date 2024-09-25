package play.modules.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PdfResultTest {
  @Test
  public void contentDisposition_inline() {
    PdfResult pdfResult = new PdfResult().inline(true);

    assertThat(pdfResult.contentDisposition("hello.pdf"))
        .isEqualTo("inline; filename=\"hello.pdf\"");
  }

  @Test
  public void contentDisposition_nonInline() {
    PdfResult pdfResult = new PdfResult().inline(false);

    assertThat(pdfResult.contentDisposition("hello.pdf"))
        .isEqualTo("attachment; filename=\"hello.pdf\"");
  }
}
