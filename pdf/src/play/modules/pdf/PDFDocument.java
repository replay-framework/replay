package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PDFDocument {
  public final String filename;
  public final String template;
  public final PDF.Options options;
  public final Map<String, Object> args = new HashMap<>();
  private final List<IHtmlToPdfTransformer.CHeaderFooter> headerFooterList = new LinkedList<>();
  String content;

  public PDFDocument(String template, PDF.Options options, String filename) {
    this.template = template;
    this.options = options;
    this.filename = filename;
  }

  void addHeaderFooter(IHtmlToPdfTransformer.CHeaderFooter headerFooter) {
    headerFooterList.add(headerFooter);
  }

  List<IHtmlToPdfTransformer.CHeaderFooter> getHeaderFooterList() {
    return headerFooterList;
  }
}
