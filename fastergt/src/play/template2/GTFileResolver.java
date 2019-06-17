package play.template2;

import javax.annotation.Nullable;

public class GTFileResolver {

    /**
     * This must be set by the framework with a working resolver
     */
    public static Resolver impl;

    public interface Resolver {
        @Nullable GTTemplateLocationReal getTemplateLocationReal(String queryPath);
        @Nullable GTTemplateLocationReal getTemplateLocationFromRelativePath(String relativePath);
    }
}
