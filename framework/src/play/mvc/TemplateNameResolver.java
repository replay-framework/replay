package play.mvc;

public class TemplateNameResolver {
  public String resolveTemplateName() {
    Http.Request request = Http.Request.current();
    String format = request == null ? null : request.format;
    String templateName = request.action.replace(".", "/") + "." + (format == null ? "html" : format);
    return resolveTemplateName(templateName);
  }

  public String resolveTemplateName(String templateName) {
    Http.Request request = Http.Request.current();
    String format = request == null ? null : request.format;
    if (templateName.startsWith("@")) {
      templateName = templateName.substring(1);
      if (!templateName.contains(".")) {
        templateName = request.controller + "." + templateName;
      }
      templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
    }
    return templateName;
  }
}
