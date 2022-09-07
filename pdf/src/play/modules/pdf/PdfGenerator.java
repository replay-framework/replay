package play.modules.pdf;

import play.mvc.Http;
import play.mvc.TemplateNameResolver;

import javax.inject.Singleton;
import java.io.ByteArrayOutputStream;

@Singleton
public class PdfGenerator {
  private static final TemplateNameResolver templateNameResolver = new TemplateNameResolver();
  private static final PdfHelper helper = new PdfHelper();

  public byte[] generate(PdfTemplate pdfTemplate) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PDFDocumentRequest request = helper.createSinglePDFDocuments(pdfTemplate);
    PDFDocument document = helper.generatePdfFromTemplate(pdfTemplate, request);
    helper.renderPDF(document, out, Http.Request.current());
    return out.toByteArray();
  }
}
