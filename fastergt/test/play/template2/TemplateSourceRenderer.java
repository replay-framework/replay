package play.template2;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Convenient class that makes it easy to compile and render source in our tests
 */
public class TemplateSourceRenderer {

    private final GTTemplateRepo tr;

    public TemplateSourceRenderer(GTTemplateRepo templateRepo) {
        this.tr = templateRepo;
    }

    public String renderSrc(String src, Map<String, Object> args) {

        GTTemplateLocationWithEmbeddedSource tl = new GTTemplateLocationWithEmbeddedSource(src);

        GTJavaBase t = tr.getTemplateInstance(tl);

        t.renderTemplate( args );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        t.writeOutput(out, UTF_8);

        return new String(out.toByteArray(), UTF_8);
    }
}
