package play.mvc;

import static java.util.Objects.requireNonNullElse;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;

@NullMarked
@CheckReturnValue
public class TemplateNameResolver {

  public String resolveTemplateName() {
    Http.Request request = Http.Request.current();
    String templateName =
        request.action.replace(".", "/") + "." + requireNonNullElse(request.format, "html");
    return resolveTemplateName(templateName);
  }

  public String resolveTemplateName(String templateName) {
    Http.Request request = Http.Request.current();
    if (templateName.startsWith("@")) {
      templateName = templateName.substring(1);
      if (!templateName.contains(".")) {
        templateName = request.controller + "." + templateName;
      }
      templateName =
          templateName.replace(".", "/") + "." + requireNonNullElse(request.format, "html");
    }
    return templateName;
  }
}
