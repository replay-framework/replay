package play.templates.types;

import play.templates.SafeFormatter;
import play.templates.Template;

import static org.apache.commons.text.StringEscapeUtils.escapeCsv;

public class SafeCSVFormatter implements SafeFormatter {

    @Override
    public String format(Template template, Object value) {
        if (value != null) {
            return escapeCsv(value.toString());   
        }
        return "";
    }
}
