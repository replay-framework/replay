package play.mvc.results;

import org.w3c.dom.Document;
import play.exceptions.UnexpectedException;
import play.libs.XML;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

/**
 * 200 OK with a text/xml
 */
public class RenderXml extends Result {

    private final String xml;

    public RenderXml(CharSequence xml) {
        this.xml = xml.toString();
    }

    public RenderXml(Document document) {
        this.xml = XML.serialize(document);
    }

    @Override
    public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
        try {
            setContentTypeIfNotSet(response, "text/xml");
            response.out.write(xml.getBytes(response.encoding));
        } catch(Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getXml() {
        return xml;
    }
}
