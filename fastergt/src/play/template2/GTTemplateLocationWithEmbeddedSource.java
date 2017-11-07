package play.template2;

import java.util.concurrent.atomic.AtomicInteger;

public class GTTemplateLocationWithEmbeddedSource extends GTTemplateLocation {
    private String source;
    private static AtomicInteger nextKey = new AtomicInteger(1);

    public GTTemplateLocationWithEmbeddedSource(String relativePath, String source) {
        super(relativePath);
        this.source = source;
    }

    public GTTemplateLocationWithEmbeddedSource( String source) {
        super( generateKey() );
        this.source = source;
    }

    // returns a generated unique key
    private static String generateKey() {
        return "GTTemplateLocationWithEmbeddedSource_generated_key_"+nextKey.getAndIncrement();
    }

    @Override
    public String readSource() {
        return source;
    }
}
