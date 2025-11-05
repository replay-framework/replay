package play.modules.pdf;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

@NullMarked
@CheckReturnValue
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
