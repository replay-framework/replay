package play.template2.exceptions;

public class GTTemplateNotFound extends GTException {

    public final String queryPath;

    public GTTemplateNotFound(String queryPath) {
        super("Cannot find template file " + queryPath);
        this.queryPath = queryPath;
    }
}
