package play.mvc;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.util.Objects.requireNonNullElse;

@ParametersAreNonnullByDefault
public class TemplateNameResolver {

  @Nonnull
  @CheckReturnValue
  public String resolveTemplateName() {
    Http.Request request = Http.Request.current();
    String templateName = request.action.replace(".", "/") + "." + requireNonNullElse(request.format, "html");
    return resolveTemplateName(templateName);
  }

  @Nonnull
  @CheckReturnValue
  public String resolveTemplateName(String templateName) {
    Http.Request request = Http.Request.current();
    if (templateName.startsWith("@")) {
      templateName = templateName.substring(1);
      if (!templateName.contains(".")) {
        templateName = request.controller + "." + templateName;
      }
      templateName = templateName.replace(".", "/") + "." + requireNonNullElse(request.format, "html");
    }
    return templateName;
  }
}
