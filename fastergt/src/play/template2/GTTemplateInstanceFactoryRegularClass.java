package play.template2;

public class GTTemplateInstanceFactoryRegularClass extends GTTemplateInstanceFactory{

    private final Class<? extends GTJavaBase> templateClass;

    public GTTemplateInstanceFactoryRegularClass(Class<? extends GTJavaBase> templateClass) {
        this.templateClass = templateClass;
    }

    @Override public Class<? extends GTJavaBase> getTemplateClass() {
        return templateClass;
    }
}
