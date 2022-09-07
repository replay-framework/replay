package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.CConvertException;
import play.Play;
import play.mvc.Http;
import play.mvc.Scope;
import play.mvc.TemplateNameResolver;
import play.server.Server;
import play.templates.Template;
import play.templates.TemplateLoader;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElseGet;
import static org.allcolor.yahp.converter.IHtmlToPdfTransformer.DEFAULT_PDF_RENDERER;
import static org.apache.commons.io.FilenameUtils.getBaseName;

@ParametersAreNonnullByDefault
public class PdfHelper {
  private static final TemplateNameResolver templateNameResolver = new TemplateNameResolver();
  private static IHtmlToPdfTransformer transformer;

  public boolean isIE(Http.Request request) {
    if (!request.headers.containsKey("user-agent"))
      return false;

    Http.Header userAgent = request.headers.get("user-agent");
    return userAgent.value().contains("MSIE");
  }

  public void renderPDF(PDFDocument document, OutputStream out, @Nullable Http.Request request) {
    try {
      Map<?, ?> properties = new HashMap<>(Play.configuration);
      String uri = request == null ? "" : ("http://localhost:" + Server.httpPort + request.url);
      renderDoc(document, uri, properties, out);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void renderDoc(PDFDocument doc, String uri, Map<?, ?> properties, OutputStream out) throws CConvertException {
    getTransformer().transform(new ByteArrayInputStream(removeScripts(doc.content).getBytes(UTF_8)),
      uri, doc.pageSize, emptyList(),
      properties, out);
  }

  public synchronized IHtmlToPdfTransformer getTransformer() {
    if (transformer == null) {
      try {
        transformer = (IHtmlToPdfTransformer) Class.forName(DEFAULT_PDF_RENDERER).getDeclaredConstructor().newInstance();
      }
      catch (Exception e) {
        throw new RuntimeException("Exception initializing pdf module", e);
      }
    }

    return transformer;
  }

  public String removeScripts(String html) {
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

  public String templateNameFromAction(@Nullable String format) {
    return Http.Request.current().action.replace(".", "/") + "." + (format == null ? "html" : format);
  }

  public Map<String, Object> templateBinding(Map<String, Object> args) {
    Map<String, Object> templateBinding = new HashMap<>(args);
    Scope.RenderArgs renderArgs = Scope.RenderArgs.current();
    if (renderArgs != null) {
      templateBinding.putAll(renderArgs.data);
    }

    templateBinding.put("request", Http.Request.current());

    return templateBinding;
  }

  public PDFDocument generatePdfFromTemplate(PdfTemplate pdfTemplate, PDFDocumentRequest pdfDocumentRequest) {
    String templateName = templateNameResolver.resolveTemplateName(pdfDocumentRequest.template);
    Template template = TemplateLoader.load(templateName);
    
    Map<String, Object> args = new HashMap<>(templateBinding(pdfTemplate.getArguments()));
    String content = template.render(args);
    return new PDFDocument(content, pdfDocumentRequest.pageSize);
  }

  public PDFDocumentRequest createSinglePDFDocuments(PdfTemplate pdfTemplate) {
    String templateName = requireNonNullElseGet(pdfTemplate.getTemplateName(), () -> templateNameFromAction("html"));
    return new PDFDocumentRequest(templateName, pdfTemplate.getPageSize(), fileName(pdfTemplate.getFileName(), templateName));
  }

  public String fileName(@Nullable String providedFileName, String templateName) {
    return requireNonNullElseGet(providedFileName, () -> getBaseName(templateName) + ".pdf");
  }
}
