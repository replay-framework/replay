package play.exceptions;

import play.templates.Template;

public class TemplateException extends PlayException {
    public TemplateException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(String.format("%s in template %s:%s caused by %s", message, template.getName(), lineNumber, cause), cause);
    }

    public TemplateException(Template template, Integer lineNumber, String message) {
        super(String.format("%s in template %s:%s", message, template.getName(), lineNumber));
    }
}
