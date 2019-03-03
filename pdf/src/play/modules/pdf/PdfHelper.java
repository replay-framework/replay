package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.CConvertException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.mvc.Http;
import play.mvc.Scope;
import play.server.Server;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

class PdfHelper {
  protected static IHtmlToPdfTransformer transformer;

  void loadHeaderAndFooter(PDFDocument doc, Map<String, Object> args) throws TemplateNotFoundException {
    PDF.Options options = doc.options;
    if (options == null)
      return;
    if (!StringUtils.isEmpty(options.HEADER_TEMPLATE)) {
      Template template = TemplateLoader.load(options.HEADER_TEMPLATE);
      options.HEADER = template.render(new HashMap<>(args));
    }
    if (!StringUtils.isEmpty(options.FOOTER_TEMPLATE)) {
      Template template = TemplateLoader.load(options.FOOTER_TEMPLATE);
      options.FOOTER = template.render(new HashMap<>(args));
    }
    if (!StringUtils.isEmpty(options.HEADER))
      doc.addHeaderFooter(new IHtmlToPdfTransformer.CHeaderFooter(options.HEADER, IHtmlToPdfTransformer.CHeaderFooter.HEADER));
    if (!StringUtils.isEmpty(options.ALL_PAGES))
      doc.addHeaderFooter(new IHtmlToPdfTransformer.CHeaderFooter(options.ALL_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ALL_PAGES));
    if (!StringUtils.isEmpty(options.EVEN_PAGES))
      doc.addHeaderFooter(new IHtmlToPdfTransformer.CHeaderFooter(options.EVEN_PAGES, IHtmlToPdfTransformer.CHeaderFooter.EVEN_PAGES));
    if (!StringUtils.isEmpty(options.FOOTER))
      doc.addHeaderFooter(new IHtmlToPdfTransformer.CHeaderFooter(options.FOOTER, IHtmlToPdfTransformer.CHeaderFooter.FOOTER));
    if (!StringUtils.isEmpty(options.ODD_PAGES))
      doc.addHeaderFooter(new IHtmlToPdfTransformer.CHeaderFooter(options.ODD_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ODD_PAGES));
  }

  boolean isIE(Http.Request request) {
    if (!request.headers.containsKey("user-agent"))
      return false;

    Http.Header userAgent = request.headers.get("user-agent");
    return userAgent.value().contains("MSIE");
  }

  void renderPDF(PDFDocument document, OutputStream out, Http.Request request) {
    try {
      Map<?, ?> properties = new HashMap<>(Play.configuration);
      String uri = request == null ? "" : ("http://localhost:" + Server.httpPort + request.url);
      renderDoc(document, uri, properties, out);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void renderDoc(PDFDocument doc, String uri, Map<?, ?> properties,
                 OutputStream out) throws UnsupportedEncodingException, CConvertException {
    IHtmlToPdfTransformer.PageSize pageSize = doc.options != null ? doc.options.pageSize : IHtmlToPdfTransformer.A4P;
    getTransformer().transform(new ByteArrayInputStream(removeScripts(doc.content).getBytes("UTF-8")),
      uri, pageSize, doc.getHeaderFooterList(),
      properties, out);
  }

  private synchronized IHtmlToPdfTransformer getTransformer() {
    if (transformer == null) {
      try {
        transformer = (IHtmlToPdfTransformer) Class.forName(IHtmlToPdfTransformer.DEFAULT_PDF_RENDERER).newInstance();
      }
      catch (Exception e) {
        throw new RuntimeException("Exception initializing pdf module", e);
      }
    }

    return transformer;
  }

  String removeScripts(String html) {
    int nextScriptStart = html.indexOf("<script");
    if (nextScriptStart == -1) return html;

    StringBuilder sb = new StringBuilder(html.length() - 16);
    int i = 0;
    while (nextScriptStart > -1) {
      sb.append(html, i, nextScriptStart);
      i = html.indexOf("</script>", nextScriptStart) + 9;
      nextScriptStart = html.indexOf("<script", i);
    }
    sb.append(html, i, html.length());
    return sb.toString();
  }

  String templateNameFromAction(String format) {
    return Http.Request.current().action.replace(".", "/") + "." + (format == null ? "html" : format);
  }

  Map<String, Object> templateBinding(Map<String, Object> args) {
    Map<String, Object> templateBinding = new HashMap<>();
    templateBinding.putAll(args);
    Scope.RenderArgs renderArgs = Scope.RenderArgs.current();
    if (renderArgs != null) {
      templateBinding.putAll(renderArgs.data);
    }

    templateBinding.put("request", Http.Request.current());

    return templateBinding;
  }

  PDFDocument createSinglePDFDocuments(PdfTemplate pdfTemplate) {
    String templateName = pdfTemplate.getTemplateName() != null ? pdfTemplate.getTemplateName() : templateNameFromAction("html");
    return new PDFDocument(templateName, pdfTemplate.options(), fileName(templateName, pdfTemplate.options()));
  }

  private String fileName(String templateName, PDF.Options options) {
    return options != null && options.filename != null ?
      options.filename :
      FilenameUtils.getBaseName(templateName) + ".pdf";
  }
}
