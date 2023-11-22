package play.modules.gtengineplugin;

import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import play.Play;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http;
import play.template2.GTJavaBase;
import play.template2.GTRenderingResult;
import play.template2.GTTemplateLocation;
import play.templates.Template;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@ParametersAreNonnullByDefault
public class GTTemplate extends Template {

    private final GTTemplateLocation templateLocation;
    private final GTJavaBase gtJavaBase;

    public GTTemplate(GTTemplateLocation templateLocation, @Nullable GTJavaBase gtJavaBase) {
      super(templateLocation.relativePath);
      this.templateLocation = templateLocation;
      this.gtJavaBase = gtJavaBase;
    }

    @Override
    public void compile() {
        //Don't have to do anything here
    }

    @Override
    protected String internalRender(Map<String, Object> args) {
        GTRenderingResult renderingResult = internalGTRender(args);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderingResult.writeOutput(out, UTF_8);
        return out.toString(UTF_8);
    }

    public GTRenderingResult internalGTRender(Map<String, Object> templateArgs) {
        Map<String, Object> args = new HashMap<>(templateArgs);
        Http.Request currentResponse = Http.Request.current();
        if ( currentResponse != null) {
            args.put("_response_encoding", currentResponse.encoding);
        }
        args.put("play", new Play());
        args.put("messages", new Messages());
        args.put("lang", Lang.get());

        return renderGTTemplate(args);
    }

    protected GTJavaBase getGTTemplateInstance() {
        if (gtJavaBase == null) {
            return TemplateLoader.getGTTemplateInstance(templateLocation);
        } else {
            return gtJavaBase;
        }
    }

    protected GTRenderingResult renderGTTemplate(Map<String, Object> args) {
        GTJavaBase gtTemplate = getGTTemplateInstance();
        Monitor monitor = MonitorFactory.start(this.name);
        try {
            gtTemplate.renderTemplate(args);
            return gtTemplate;
        }
        finally {
            monitor.stop();
        }
    }

    @Override
    public String render(Map<String, Object> args) {
        return internalRender(args);
    }
}
