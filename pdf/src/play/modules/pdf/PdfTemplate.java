package play.modules.pdf;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

@ParametersAreNonnullByDefault
public class PdfTemplate {
  @Nullable private final String templateName;
  private final Map<String, Object> arguments = new HashMap<>();
  private String fileName;
  private IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;

  public PdfTemplate() {
    this(null);
  }

  public PdfTemplate(@Nullable String templateName) {
    this.templateName = templateName;
  }

  @Nonnull
  public final PdfTemplate with(String name, Object value) {
    arguments.put(name, value);
    return this;
  }

  @Nonnull
  public final PdfTemplate fileName(String fileName) {
    this.fileName = requireNonNull(fileName);
    return this;
  }

  @Nonnull
  public final PdfTemplate pageSize(IHtmlToPdfTransformer.PageSize pageSize) {
    this.pageSize = requireNonNull(pageSize);
    return this;
  }

  @Nonnull
  @CheckReturnValue
  public Map<String, Object> getArguments() {
    return arguments;
  }

  @Nullable
  public String getTemplateName() {
    return templateName;
  }

  @Nonnull
  @CheckReturnValue
  public IHtmlToPdfTransformer.PageSize getPageSize() {
    return pageSize;
  }

  @Nullable
  public String getFileName() {
    return fileName;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof PdfTemplate)) return false;

    PdfTemplate other = (PdfTemplate) object;
    return Objects.equals(templateName, other.templateName)
        && Objects.equals(arguments, other.arguments)
        && Objects.equals(fileName, other.fileName)
        && Objects.equals(pageSize, other.pageSize);
  }

  @Override
  public String toString() {
    return String.format(
        "PdfTemplate {%s, %s, %s, %s}", templateName, fileName, pageSize, arguments);
  }
}
