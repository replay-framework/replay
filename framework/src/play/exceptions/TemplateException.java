package play.exceptions;

import play.templates.Template;

/**
 * An exception during template execution
 */
public abstract class TemplateException extends PlayException implements SourceAttachment {

    private Template template;
    private Integer lineNumber;

    public TemplateException(Template template, Integer lineNumber, String message) {
        super(message);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public TemplateException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public Template getTemplate() {
        return template;
    }

    @Override
    public Integer getLineNumber() {
        return lineNumber;
    }

    @Override
    public String getSourceFile() {
        return template.name;
    }
}
