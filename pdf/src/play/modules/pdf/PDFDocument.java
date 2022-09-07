package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

import javax.annotation.ParametersAreNonnullByDefault;

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
