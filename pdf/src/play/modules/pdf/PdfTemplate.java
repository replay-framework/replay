package play.modules.pdf;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.allcolor.yahp.converter.IHtmlToPdfTransformer;

@NullMarked
@CheckReturnValue
@SuppressWarnings({"NonFinalFieldReferencedInHashCode", "NonFinalFieldReferenceInEquals"})
public class PdfTemplate {
  @Nullable
  private final String templateName;
  private final Map<String, Object> arguments = new HashMap<>();
  @Nullable
  private String fileName;
  private IHtmlToPdfTransformer.PageSize pageSize = IHtmlToPdfTransformer.A4P;

  public PdfTemplate() {
    this(null);
  }

  public PdfTemplate(@Nullable String templateName) {
    this.templateName = templateName;
  }

  @CanIgnoreReturnValue
  public final PdfTemplate with(String name, Object value) {
    arguments.put(name, value);
    return this;
  }

  @CanIgnoreReturnValue
  public final PdfTemplate fileName(String fileName) {
    this.fileName = requireNonNull(fileName);
    return this;
  }

  @CanIgnoreReturnValue
  public final PdfTemplate pageSize(IHtmlToPdfTransformer.PageSize pageSize) {
    this.pageSize = requireNonNull(pageSize);
    return this;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  @Nullable
  public String getTemplateName() {
    return templateName;
  }

  public IHtmlToPdfTransformer.PageSize getPageSize() {
    return pageSize;
  }

  @Nullable
  public String getFileName() {
    return fileName;
  }

  @Override
  public boolean equals(Object object) {
    if (!(object instanceof PdfTemplate other)) return false;

    return Objects.equals(templateName, other.templateName)
        && Objects.equals(arguments, other.arguments)
        && Objects.equals(fileName, other.fileName)
        && Objects.equals(pageSize, other.pageSize);
  }

  @Override
  public int hashCode() {
    return Objects.hash(templateName, arguments, fileName, pageSize);
  }

  @Override
  public String toString() {
    return String.format(
        "PdfTemplate {%s, %s, %s, %s}", templateName, fileName, pageSize, arguments);
  }
}
