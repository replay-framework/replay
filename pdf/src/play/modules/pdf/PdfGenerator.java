package play.modules.pdf;

import jakarta.inject.Singleton;
import play.mvc.Http;

import java.io.ByteArrayOutputStream;

@Singleton
public class PdfGenerator {
  private static final PdfHelper helper = new PdfHelper();

  public byte[] generate(PdfTemplate pdfTemplate) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PDFDocument document = helper.generatePDF(pdfTemplate);
    helper.renderPDF(document, out, Http.Request.current());
    return out.toByteArray();
  }
}
