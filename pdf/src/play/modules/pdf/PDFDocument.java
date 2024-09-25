package play.modules.pdf;

import javax.annotation.ParametersAreNonnullByDefault;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

@ParametersAreNonnullByDefault
public class PDFDocument {
  final String content;
  final IHtmlToPdfTransformer.PageSize pageSize;

  public PDFDocument(String content) {
    this(content, IHtmlToPdfTransformer.A4P);
  }

  public PDFDocument(String content, IHtmlToPdfTransformer.PageSize pageSize) {
    this.content = content;
    this.pageSize = pageSize;
  }
}
