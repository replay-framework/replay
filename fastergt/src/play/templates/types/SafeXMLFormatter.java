package play.templates.types;

import play.templates.SafeFormatter;
import play.templates.Template;

import static org.apache.commons.text.StringEscapeUtils.escapeXml11;

public class SafeXMLFormatter implements SafeFormatter {

    @Override
    public String format(Template template, Object value) {
        if (value != null) {
            return escapeXml11(value.toString());
        }
        return "";
    }
}
