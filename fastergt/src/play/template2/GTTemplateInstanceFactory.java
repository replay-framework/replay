package play.template2;

import play.template2.exceptions.GTException;

public abstract class GTTemplateInstanceFactory {

    public GTJavaBase create(GTTemplateRepo templateRepo) {
        try {
            GTJavaBase templateInstance = getTemplateClass().newInstance();
            // Must tell the template Instance where the current templateRepo is - needed when processing #{extends} and custom tags
            templateInstance.templateRepo = templateRepo;
            return templateInstance;
        } catch (Exception e) {
            throw new GTException("Error creating template instance", e);
        }
    }

    public abstract Class<? extends GTJavaBase> getTemplateClass();
}
