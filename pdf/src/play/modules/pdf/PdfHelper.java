package play.modules.pdf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNullElseGet;
import static org.allcolor.yahp.converter.IHtmlToPdfTransformer.DEFAULT_PDF_RENDERER;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static play.libs.Lazy.lazyEvaluated;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.CConvertException;
import play.Play;
import play.libs.Lazy;
import play.mvc.Http;
import play.mvc.Scope;
import play.mvc.TemplateNameResolver;
import play.templates.Template;
import play.templates.TemplateLoader;

@NullMarked
@CheckReturnValue
public class PdfHelper {
  private static final TemplateNameResolver templateNameResolver = new TemplateNameResolver();
  private static final Lazy<IHtmlToPdfTransformer> transformer = lazyEvaluated(() -> initTransformer());

  public boolean isIE(Http.Request request) {
    if (!request.headers.containsKey("user-agent")) return false;

    Http.Header userAgent = request.headers.get("user-agent");
    return userAgent.value().contains("MSIE");
  }

  public void renderPDF(PDFDocument document, OutputStream out, Http.@Nullable Request request) {
    try {
      Map<?, ?> properties = new HashMap<>(Play.configuration);
      String uri = request == null ? "" : (request.getBase() + request.url);
      renderDoc(document, uri, properties, out);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void renderDoc(PDFDocument doc, String uri, Map<?, ?> properties, OutputStream out)
      throws CConvertException {
    transformer.get()
        .transform(
            new ByteArrayInputStream(removeScripts(doc.content).getBytes(UTF_8)),
            uri,
            doc.pageSize,
            emptyList(),
            properties,
            out);
  }

  private static synchronized IHtmlToPdfTransformer initTransformer() {
    try {
      return (IHtmlToPdfTransformer) Class.forName(DEFAULT_PDF_RENDERER).getDeclaredConstructor()
          .newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Exception initializing pdf module", e);
    }
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

  private Map<String, Object> templateBinding(Map<String, Object> args) {
    Map<String, Object> templateBinding = new HashMap<>(args);
    Scope.RenderArgs renderArgs = Scope.RenderArgs.current();
    if (renderArgs != null) {
      templateBinding.putAll(renderArgs.data);
    }

    templateBinding.put("request", Http.Request.current());

    return templateBinding;
  }

  public PDFDocument generatePDF(PdfTemplate pdfTemplate) {
    String templateName = templateName(pdfTemplate);
    Template htmlTemplate = TemplateLoader.load(templateName);

    Map<String, Object> args = new HashMap<>(templateBinding(pdfTemplate.getArguments()));
    String content = htmlTemplate.render(args);
    return new PDFDocument(content, pdfTemplate.getPageSize());
  }

  private String templateName(PdfTemplate pdfTemplate) {
    return templateNameResolver.resolveTemplateName(pdfTemplate.getTemplateName());
  }

  String fileName(PdfTemplate pdfTemplate) {
    return fileName(pdfTemplate.getFileName(), templateName(pdfTemplate));
  }

  private String fileName(@Nullable String providedFileName, String templateName) {
    return requireNonNullElseGet(providedFileName, () -> getBaseName(templateName) + ".pdf");
  }
}
