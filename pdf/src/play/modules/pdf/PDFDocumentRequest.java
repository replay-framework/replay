package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class PDFDocumentRequest {
  public final String template;
  public final IHtmlToPdfTransformer.PageSize pageSize;
  public final String filename;

  public PDFDocumentRequest(String template, IHtmlToPdfTransformer.PageSize pageSize, String filename) {
    this.template = template;
    this.pageSize = pageSize;
    this.filename = filename;
  }
}
