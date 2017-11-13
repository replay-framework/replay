package play.rebel;

import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http;
import play.mvc.Scope;
import play.mvc.Scope.Session;
import play.mvc.results.Result;
import play.templates.Template;
import play.templates.TemplateLoader;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;

/**
 * 200 OK with a template rendering
 */
public class View extends Result {
  private static TemplateNameResolver templateNameResolver = new TemplateNameResolver();

  private final String templateName;
  private final Map<String, Object> arguments = new HashMap<>();
  private String content;
  private long renderTime;

  public View() {
    this(templateNameResolver.resolveTemplateName());
  }

  public View(String templateName) {
    this(templateName, emptyMap());
  }

  public View(String templateName, Map<String, Object> arguments) {
    this.templateName = templateName;
    this.arguments.putAll(Scope.RenderArgs.current().data);
    this.arguments.putAll(arguments);
  }

  @Override
  public void apply(Http.Request request, Http.Response response) {
    try {
      renderView(response);
    }
    catch (Exception e) {
      throw new UnexpectedException(e);
    }
    finally {
      // we need to store session if authenticity token has been generated during rendering html
      Session session = Session.current();
      if (isChanged(session)) {
        save(session);
      }
    }
  }

  private boolean isChanged(Session session) {
    try {
      Field field = Session.class.getDeclaredField("changed");
      field.setAccessible(true);
      return (boolean) field.get(session);
    }
    catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void save(Session session) {
    try {
      Method method = Session.class.getDeclaredMethod("save");
      method.setAccessible(true);
      method.invoke(session);
    }
    catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  private void renderView(Http.Response response) throws IOException {
    long start = System.currentTimeMillis();
    Template template = resolveTemplate();

    Map<String, Object> templateBinding = new HashMap<>();
    templateBinding.putAll(arguments);
    templateBinding.put("session", Session.current());
    templateBinding.put("request", Http.Request.current());
    templateBinding.put("flash", Scope.Flash.current());
    templateBinding.put("params", Scope.Params.current());
    templateBinding.put("errors", Validation.errors());

    this.content = template.render(templateBinding);
    this.renderTime = System.currentTimeMillis() - start;
    String contentType = MimeTypes.getContentType(template.name, "text/plain");
    response.out.write(content.getBytes(getEncoding()));
    setContentTypeIfNotSet(response, contentType);
  }

  private Template resolveTemplate() {
    return TemplateLoader.load(templateNameResolver.resolveTemplateName(templateName));
  }
  
  public String getName() {
    return templateName;
  }

  public String getContent() {
    return content;
  }

  public Map<String, Object> getArguments() {
    return arguments;
  }

  public long getRenderTime() {
    return renderTime;
  }

  public View with(String name, Object value) {
    arguments.put(name, value);
    return this;
  }
}

