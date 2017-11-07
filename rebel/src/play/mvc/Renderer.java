package play.mvc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import play.data.validation.Validation;
import play.templates.Template;
import play.templates.TemplateLoader;

import javax.inject.Singleton;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static play.mvc.Controller.*;

@Singleton
public class Renderer {
  private static final Gson gsonWithTimeFormat = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();
  
  public void template() {
    renderTemplate(Controller.template(), emptyMap());
  }
  
  public void template(String templateName) {
    renderTemplate(templateName, emptyMap());
  }

  public void template(String templateName, Map<String, Object> args) {
    renderTemplate(templateName, args);
  }

  public Builder with(String name, Object value) {
    return new Builder(name, value);
  }

  public void text(String text) {
    renderText(text);
  }

  public void html(String html) {
    renderHtml(html);
  }

  public void xml(Object arg) {
    renderXml(arg);
  }

  public void json(Object arg) {
    if (arg instanceof String)
      renderJSON((String) arg);
    else
      renderJSON(arg, gsonWithTimeFormat);
  }

  public void file(File file, String fileName) {
    renderBinary(file, fileName);
  }

  public String templateAsString(String templateName) {
    Scope.RenderArgs templateBinding = Scope.RenderArgs.current();
    templateBinding.data.putAll(Scope.RenderArgs.current().data);
    templateBinding.put("session", Scope.Session.current());
    templateBinding.put("request", Http.Request.current());
    templateBinding.put("flash", Scope.Flash.current());
    templateBinding.put("params", Scope.Params.current());
    templateBinding.put("errors", Validation.errors());
    Template t = TemplateLoader.load(templateName);
    return t.render(templateBinding.data);
  }

  public class Builder {
    private final Map<String, Object> arguments = new HashMap<>();

    private Builder(String name, Object value) {
      with(name, value);
    }

    public final Builder with(String name, Object value) {
      arguments.put(name, value);
      return this;
    }

    public void template() {
      renderTemplate(Controller.template(), arguments);
    }

    public void template(String templateName) {
      renderTemplate(templateName, arguments);
    }
    
    public void json() {
      renderJSON(gsonWithTimeFormat.toJson(arguments));
    }

    public void json(Gson customGson) {
      renderJSON(customGson.toJson(arguments));
    }
  }

  public void renderTemplate(String templateName, Map<String, Object> args) {
    Controller.renderTemplate(templateName, args);
  }
}
