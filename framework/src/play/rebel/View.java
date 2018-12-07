package play.rebel;

import play.data.validation.Validation;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.TemplateNameResolver;
import play.mvc.results.Result;
import play.templates.Template;
import play.templates.TemplateLoader;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * 200 OK with a template rendering
 */
public class View extends Result {
  private static TemplateNameResolver templateNameResolver = new TemplateNameResolver();

  private final String templateName;
  private final Map<String, Object> arguments;
  private String content;
  private long renderTime;

  public View() {
    this(templateNameResolver.resolveTemplateName());
  }

  public View(String templateName) {
    this(templateName, new HashMap<>());
  }

  public View(String templateName, Map<String, Object> arguments) {
    this.templateName = templateName;
    this.arguments = arguments;
  }

  @Override
  public void apply(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
    try {
      renderView(request, response, session, renderArgs, flash);
    }
    catch (IOException e) {
      throw new UnexpectedException(e);
    }
    finally {
      // we need to store session if authenticity token has been generated during rendering html
      if (isChanged(session)) {
        session.save(request, response);
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

  private void renderView(Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) throws IOException {
    long start = System.currentTimeMillis();
    Template template = resolveTemplate();

    Map<String, Object> templateBinding = new HashMap<>();
    templateBinding.putAll(renderArgs.data);
    templateBinding.putAll(arguments);
    templateBinding.put("session", session);
    templateBinding.put("request", request);
    templateBinding.put("flash", flash);
    templateBinding.put("params", request.params);
    templateBinding.put("errors", Validation.errors());

    this.content = template.render(templateBinding);
    this.renderTime = System.currentTimeMillis() - start;
    String contentType = MimeTypes.getContentType(template.name, "text/plain");
    response.out.write(content.getBytes(response.encoding));
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
    Map<String, Object> combinedArguments = new HashMap<>(RenderArgs.current().data);
    combinedArguments.putAll(arguments);
    return combinedArguments;
  }

  public long getRenderTime() {
    return renderTime;
  }

  public View with(String name, @Nullable Object value) {
    arguments.put(name, value);
    return this;
  }
}

