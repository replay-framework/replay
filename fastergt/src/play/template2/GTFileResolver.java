package play.template2;

public class GTFileResolver {

    /**
     * This must be set by the framework with a working resolver
     */
    public static Resolver impl;

    public interface Resolver {
        GTTemplateLocationReal getTemplateLocationReal(String queryPath);
        GTTemplateLocationReal getTemplateLocationFromRelativePath(String relativePath);
    }
}
