package play.exceptions;

import play.templates.Template;

public class TemplateNotFoundException extends PlayException implements SourceAttachment {

    private final String path;
    private String sourceFile;
    private Integer line;

    public TemplateNotFoundException(String path) {
        super("Template not found : " + path);
        this.path = path;
    }

    public TemplateNotFoundException(String path, Template template, Integer line) {
        this(path);
        if (template != null) {
            this.sourceFile = template.name;
        }
        this.line = line;
    }

    public String getPath() {
        return this.path;
    }

    @Override
    public String getErrorTitle() {
        return "Template not found";
    }

    @Override
    public String getErrorDescription() {
        return String.format("The template <strong>%s</strong> does not exist.", this.path);
    }

    @Override
    public String getSourceFile() {
        return this.sourceFile;
    }

    @Override
    public Integer getLineNumber() {
        return this.line;
    }
}