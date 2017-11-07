package play.template2;

import java.net.URL;

public class GTTemplateLocationReal extends GTTemplateLocation {

    public final URL realFileURL;

    public GTTemplateLocationReal(String relativePath, URL realFileURL) {
        super(relativePath);
        this.realFileURL = realFileURL;
    }

    @Override
    public String readSource() {
        return IO.readContentAsString(realFileURL);
    }

    @Override
    public String toString() {
        return "GTTemplateLocationReal{" +
                "realFile=" + realFileURL +
                "} " + super.toString();
    }
}
