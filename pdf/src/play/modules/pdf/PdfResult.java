package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.TemplateNameResolver;
import play.mvc.results.Result;

public class PdfResult extends Result {
  private static final TemplateNameResolver templateNameResolver = new TemplateNameResolver();
  private static final PdfHelper helper = new PdfHelper();

  private final PdfTemplate pdfTemplate;
  private boolean inline = true;

  public PdfResult() {
    this(new PdfTemplate());
  }

  public PdfResult(String templateName) {
    this(new PdfTemplate(templateName));
  }

  public PdfResult(PdfTemplate pdfTemplate) {
    this.pdfTemplate = pdfTemplate;
  }

  public PdfResult inline(boolean inline) {
    this.inline = inline;
    return this;
  }

  public PdfTemplate getTemplate() {
    return pdfTemplate;
  }

  @Override public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
    renderArgs.put("session", session);
    renderArgs.put("flash", flash);

    PDFDocument document = helper.createSinglePDFDocuments(pdfTemplate);
    response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + document.filename + "\"");
    setContentTypeIfNotSet(response, "application/pdf");
    // FIX IE bug when using SSL
    if (request.secure && helper.isIE(request))
      response.setHeader("Cache-Control", "");

    helper.generatePdfFromTemplate(pdfTemplate, document);
    helper.renderPDF(document, response.out, request);
  }

  public PdfResult with(String name, Object value) {
    pdfTemplate.with(name, value);
    return this;
  }

  public PdfResult fileName(String fileName) {
    pdfTemplate.fileName(fileName);
    return this;
  }

  public PdfResult pageSize(IHtmlToPdfTransformer.PageSize pageSize) {
    pdfTemplate.pageSize(pageSize);
    return this;
  }

  @Override
  public boolean isRenderingTemplate() {
      return true;
  }
}
