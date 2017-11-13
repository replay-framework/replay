package play.rebel;

import play.mvc.Http;

public class TemplateNameResolver {
  public String resolveTemplateName() {
    Http.Request theRequest = Http.Request.current();
    String format = theRequest.format;
    String templateName = theRequest.action.replace(".", "/") + "." + (format == null ? "html" : format);
    return resolveTemplateName(templateName);
  }

  public String resolveTemplateName(String templateName) {
    Http.Request theRequest = Http.Request.current();
    String format = theRequest.format;
    if (templateName.startsWith("@")) {
      templateName = templateName.substring(1);
      if (!templateName.contains(".")) {
        templateName = theRequest.controller + "." + templateName;
      }
      templateName = templateName.replace(".", "/") + "." + (format == null ? "html" : format);
    }
    return templateName;
  }
}
