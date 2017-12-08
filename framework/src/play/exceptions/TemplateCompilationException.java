package play.exceptions;

import play.templates.Template;

/**
 * A exception during template compilation
 */
public class TemplateCompilationException extends TemplateException {

    public TemplateCompilationException(Template template, Integer lineNumber, String message) {
        super(template, lineNumber, message);
    }

    public TemplateCompilationException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(template, lineNumber, message, cause);
    }

    @Override
    public String getErrorTitle() {
        return "Template compilation error";
    }

    @Override
    public String getErrorDescription() {
        return String.format("The template <strong>%s</strong> does not compile : <strong>%s</strong>", getTemplate().name, getMessage());
    }
}
