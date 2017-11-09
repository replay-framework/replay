package play.modules.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfReader;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer.PageSize;
import org.apache.commons.lang.StringUtils;
import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.exceptions.UnexpectedException;
import play.modules.pdf.PDF.MultiPDFDocuments;
import play.modules.pdf.PDF.Options;
import play.modules.pdf.PDF.PDFDocument;
import play.mvc.Http;
import play.mvc.Http.Header;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.results.Result;
import play.server.Server;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;


public class RenderPDFTemplate extends Result {

  protected static IHtmlToPdfTransformer transformer;

  private final MultiPDFDocuments docs;
  private final boolean inline;
  private final Map<String, Object> arguments;

  public RenderPDFTemplate(MultiPDFDocuments docs, boolean inline, Map<String, Object> args) throws TemplateNotFoundException {
    this.docs = docs;
    this.inline = inline;
    this.arguments = args;
    renderDocuments(args);
  }

  private void renderDocuments(Map<String, Object> args) {
    for (PDFDocument doc : docs.documents) {
      Request request = Http.Request.current();
      String format = request == null ? null : request.format;
      String templateName = PDF.resolveTemplateName(doc.template, request, format);
      Template template = TemplateLoader.load(templateName);
      doc.args.putAll(args);
      doc.content = template.render(new HashMap<>(doc.args));
      loadHeaderAndFooter(doc, doc.args);
    }
  }

  private void loadHeaderAndFooter(PDFDocument doc, Map<String, Object> args) throws TemplateNotFoundException {
    Options options = doc.options;
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
      doc.headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.HEADER, IHtmlToPdfTransformer.CHeaderFooter.HEADER));
    if (!StringUtils.isEmpty(options.ALL_PAGES))
      doc.headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ALL_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ALL_PAGES));
    if (!StringUtils.isEmpty(options.EVEN_PAGES))
      doc.headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.EVEN_PAGES, IHtmlToPdfTransformer.CHeaderFooter.EVEN_PAGES));
    if (!StringUtils.isEmpty(options.FOOTER))
      doc.headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.FOOTER, IHtmlToPdfTransformer.CHeaderFooter.FOOTER));
    if (!StringUtils.isEmpty(options.ODD_PAGES))
      doc.headerFooterList.add(new IHtmlToPdfTransformer.CHeaderFooter(options.ODD_PAGES, IHtmlToPdfTransformer.CHeaderFooter.ODD_PAGES));
  }

  @Override public void apply(Request request, Response response) {
    try {
      response.setHeader("Content-Disposition", (inline ? "inline" : "attachment") + "; filename=\"" + docs.filename + "\"");
      setContentTypeIfNotSet(response, "application/pdf");
      // FIX IE bug when using SSL
      if (request.secure && isIE(request))
        response.setHeader("Cache-Control", "");

      renderPDF(response.out, request);
    }
    catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }

  private boolean isIE(Request request) {
    if (!request.headers.containsKey("user-agent"))
      return false;

    Header userAgent = request.headers.get("user-agent");
    return userAgent.value().contains("MSIE");
  }

  private void renderPDF(OutputStream out, Request request) throws Exception {
    Map<?, ?> properties = new HashMap<>(Play.configuration);
    String uri = request == null ? "" : ("http://localhost:" + Server.httpPort + request.url);
    if (docs.documents.size() == 1) {
      renderDoc(docs.documents.get(0), uri, properties, out);
    }
    else {
      // we need to concatenate them all
      Document resultDocument = new Document();
      PdfCopy copy = new PdfCopy(resultDocument, out);
      resultDocument.open();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      for (PDFDocument doc : docs.documents) {
        os.reset();
        renderDoc(doc, uri, properties, os);
        PdfReader pdfReader = new PdfReader(os.toByteArray());
        int n = pdfReader.getNumberOfPages();
        for (int i = 0; i < n; i++) {
          copy.addPage(copy.getImportedPage(pdfReader, i + 1));
        }
        copy.freeReader(pdfReader);
      }
      resultDocument.close();
    }
  }

  private void renderDoc(PDFDocument doc, String uri, Map<?, ?> properties,
                         OutputStream out) throws Exception {
    PageSize pageSize = doc.options != null ? doc.options.pageSize : IHtmlToPdfTransformer.A4P;
    getTransformer().transform(new ByteArrayInputStream(removeScripts(doc.content).getBytes("UTF-8")),
        uri, pageSize, doc.headerFooterList,
        properties, out);
  }

  private synchronized IHtmlToPdfTransformer getTransformer() {
    if (transformer == null) {
      try {
        transformer = (IHtmlToPdfTransformer) Play.classloader.loadClass(IHtmlToPdfTransformer.DEFAULT_PDF_RENDERER)
          .newInstance();
      }
      catch (Exception e) {
        throw new RuntimeException("Exception initializing pdf module", e);
      }
    }

    return transformer;
  }

  public void writePDF(OutputStream out, Request request) {
    try {
      renderPDF(out, request);
    }
    catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }

  static String removeScripts(String html) {
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

  public Map<String, Object> getArguments() {
    return arguments;
  }
}
