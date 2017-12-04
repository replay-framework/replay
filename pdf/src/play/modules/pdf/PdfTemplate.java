package play.modules.pdf;

import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PdfTemplate {
  private final String templateName;
  private final Map<String, Object> arguments = new HashMap<>();
  private String fileName;
  private IHtmlToPdfTransformer.PageSize pageSize;

  public PdfTemplate() {
    this(null);
  }

  public PdfTemplate(String templateName) {
    this.templateName = templateName;
  }

  public final PdfTemplate with(String name, Object value) {
    arguments.put(name, value);
    return this;
  }

  public final PdfTemplate fileName(String fileName) {
    this.fileName = fileName;
    return this;
  }

  public final PdfTemplate pageSize(IHtmlToPdfTransformer.PageSize pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  PDF.Options options() {
    if (fileName == null && pageSize == null) return null;
    PDF.Options options = new PDF.Options();
    if (fileName != null) options.filename = fileName;
    if (pageSize != null) options.pageSize = pageSize;
    return options;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  public String getTemplateName() {
    return templateName;
  }

  public String getFileName() {
    return fileName;
  }

  @Override public boolean equals(Object object) {
    if (!(object instanceof PdfTemplate)) return false;

    PdfTemplate other = (PdfTemplate) object;
    return Objects.equals(templateName, other.templateName)
      && Objects.equals(arguments, other.arguments)
      && Objects.equals(fileName, other.fileName)
      && Objects.equals(pageSize, other.pageSize);
  }

  @Override public String toString() {
    return String.format("PdfTemplate {%s, %s, %s, %s}", templateName, fileName, pageSize, arguments);
  }
}
