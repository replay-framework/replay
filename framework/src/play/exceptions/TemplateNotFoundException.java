package play.exceptions;

import play.templates.BaseTemplate;

public class TemplateNotFoundException extends PlayException {

    private final String path;

    public TemplateNotFoundException(String path) {
        super("Template not found : " + path);
        this.path = path;
    }

    public TemplateNotFoundException(String path, BaseTemplate template, int fromLine) {
        super(String.format("Template not found : %s - called from %s:%s", path, template.name, fromLine));
        this.path = path;
    }

    public String getPath() {
        return this.path;
    }
}