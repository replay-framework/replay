package play.template2;

import java.net.URL;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.DOTALL;

public class GTTemplateLocationReal extends GTTemplateLocation {

    private static final Pattern scriptBlockRegex = Pattern.compile("(<script.*?>.*?\\S.*?</script>)", DOTALL);
    public final URL realFileURL;

    public GTTemplateLocationReal(String relativePath, URL realFileURL) {
        super(relativePath);
        this.realFileURL = realFileURL;
    }

    @Override
    public String readSource() {
        String originalHtml = IO.readContentAsString(realFileURL);
        return addInlineScriptTag(originalHtml);
    }

    String addInlineScriptTag(String originalHtml) {
        return scriptBlockRegex.matcher(originalHtml).replaceAll("#{secureInlineJavaScript}$1#{/secureInlineJavaScript}");
    }

    @Override
    public String toString() {
        return "GTTemplateLocationReal{" +
                "realFile=" + realFileURL +
                "} " + super.toString();
    }
}
