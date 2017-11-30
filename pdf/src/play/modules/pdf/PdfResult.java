package play.modules.pdf;

import play.mvc.Http;
import play.mvc.TemplateNameResolver;
import play.mvc.results.Result;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.util.HashMap;

public class PdfResult extends Result {
  private static final TemplateNameResolver templateNameResolver = new TemplateNameResolver();
  private static final PdfHelper helper = new PdfHelper();

  private final PdfTemplate pdfTemplate;
  private boolean inline = true;

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

  @Override public void apply(Http.Request request, Http.Response response) {
    PDFDocument document = helper.createSinglePDFDocuments(pdfTemplate);
    response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + document.filename + "\"");
    setContentTypeIfNotSet(response, "application/pdf");
    // FIX IE bug when using SSL
    if (request.secure && helper.isIE(request))
      response.setHeader("Cache-Control", "");

    String templateName1 = templateNameResolver.resolveTemplateName(document.template);
    Template template = TemplateLoader.load(templateName1);
    document.args.putAll(helper.templateBinding(pdfTemplate.getArguments()));
    document.content = template.render(new HashMap<>(document.args));
    helper.loadHeaderAndFooter(document, document.args);
    helper.renderPDF(document, response.out, request);
  }
}
