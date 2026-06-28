package play.templates;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static play.utils.HTML.htmlEscape;

import groovy.lang.Closure;
import groovy.lang.MissingPropertyException;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.apache.commons.beanutils.PropertyUtils;
import org.codehaus.groovy.runtime.NullObject;
import play.data.validation.Error;
import play.data.validation.Validation;
import play.exceptions.TagInternalException;
import play.exceptions.TemplateException;
import play.mvc.Http;
import play.mvc.Router.ActionDefinition;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Session;
import play.utils.UuidGenerator;

public class FastTags {
  private final UuidGenerator uuidGenerator;

  public FastTags() {
    this(new UuidGenerator());
  }

  FastTags(UuidGenerator uuidGenerator) {
    this.uuidGenerator = uuidGenerator;
  }

  public void _jsRoute(
      Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
    Object arg = args.get("arg");
    if (!(arg instanceof ActionDefinition)) {
      throw new TemplateException(
          template.template,
          fromLine,
          "Wrong parameter type, try #{jsRoute @Application.index() /}",
          new TagInternalException("Wrong parameter type"));
    }
    ActionDefinition action = (ActionDefinition) arg;
    out.print("{");
    if (action.args.isEmpty()) {
      out.print("url: function() { return '" + action.url.replace("&amp;", "&") + "'; },");
    } else {
      out.print(
          "url: function(args) { var pattern = '"
              + action.url.replace("&amp;", "&")
              + "'; for (var key in args) { pattern = pattern.replace(':'+key, args[key] || ''); } return pattern; },");
    }
    out.print("method: '" + action.method + "'");
    out.print("}");
  }

  public void _authenticityToken(
      Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
    out.printf(
        "<input type=\"hidden\" name=\"authenticityToken\" value=\"%s\"/>\n",
        session(template).getAuthenticityToken());
  }

  private void addFormId(PrintWriter out) {
    out.printf(
        "<input type=\"hidden\" name=\"___form_id\" value=\"form:%s\"/>\n",
        uuidGenerator.randomUUID());
  }

  private static Session session(ExecutableTemplate template) {
    return (Session) template.getProperty("session");
  }

  /**
   * Generates a html form element linked to a controller action
   *
   * @param args tag attributes
   * @param body tag inner body
   * @param out the output writer
   * @param template enclosing template
   * @param fromLine template line number where the tag is defined
   */
  public void _form(
      Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
    ActionDefinition actionDef = null;
    Object arg = args.get("arg");
    if (arg instanceof ActionDefinition) {
      actionDef = (ActionDefinition) arg;
    } else if (arg != null) {
      actionDef = new ActionDefinition();
      actionDef.url = arg.toString();
      actionDef.method = "POST";
    }
    if (actionDef == null) {
      actionDef = (ActionDefinition) args.get("action");
    }
    String enctype = (String) args.get("enctype");
    if (enctype == null) {
      enctype = "application/x-www-form-urlencoded";
    }
    if (actionDef.star) {
      actionDef.method = "POST"; // prefer POST for form submits.
    }
    if (args.containsKey("method")) {
      actionDef.method = args.get("method").toString();
    }
    String name = null;
    if (args.containsKey("name")) {
      name = args.get("name").toString();
    }
    Charset encoding = Http.Response.current().encoding;
    out.println(
        "<form action=\""
            + htmlEscape(actionDef.url)
            + "\" method=\""
            + actionDef.method.toLowerCase()
            + "\" accept-charset=\""
            + encoding.name()
            + "\" enctype=\""
            + htmlEscape(enctype)
            + "\" "
            + serialize(args, "name", "action", "method", "accept-charset", "enctype")
            + (name != null ? "name=\"" + name + "\"" : "")
            + ">");
    if (!"GET".equals(actionDef.method)) {
      _authenticityToken(args, body, out, template, fromLine);
      addFormId(out);
    }
    out.println(JavaExtensions.toString(body));
    out.print("</form>");
  }

  /**
   * The field tag is a helper, based on the spirit of Don't Repeat Yourself.
   *
   * @param args tag attributes
   * @param body tag inner body
   * @param out the output writer
   * @param template enclosing template
   * @param fromLine template line number where the tag is defined
   */
  public void _field(
      Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
    Map<String, Object> field = new HashMap<>();
    String _arg = args.get("arg").toString();
    field.put("name", _arg);
    field.put("id", _arg.replace('.', '_'));
    field.put("flash", getArgValueFromFlash(template, _arg));
    field.put("error", Validation.error(_arg));
    field.put("errorClass", field.get("error") != null ? "hasError" : "");
    getValue(body, _arg).ifPresent(value -> field.put("value", value));
    body.setProperty("field", field);
    body.call();
  }

  Optional<Object> getValue(Closure body, String _arg) {
    if (isEmpty(_arg)) {
      // don't set any value if no arg given
      return Optional.empty();
    }

    String[] pieces = _arg.split("\\.");
    Object obj = body.getProperty(pieces[0]);
    if (obj == null) {
      return Optional.empty();
    }

    if (pieces.length <= 1) {
      return Optional.of(obj);
    }

    try {
      String path = _arg.substring(_arg.indexOf('.') + 1);
      Object value = PropertyUtils.getProperty(obj, path);
      return Optional.of(value);
    } catch (Exception e) {
      // If there is a problem reading the field we don't set any value
      return Optional.empty();
    }
  }

  @Nullable
  private static String getArgValueFromFlash(ExecutableTemplate template, String _arg) {
    try {
      return ((Flash) template.getProperty("flash")).get(_arg);
    } catch (MissingPropertyException flashIsNotBoundAndIamFineWithIt) {
      return null;
    }
  }

  /**
   * Generates a html link to a controller action
   *
   * @param args tag attributes
   * @param body tag inner body
   * @param out the output writer
   * @param template enclosing template
   * @param fromLine template line number where the tag is defined
   */
  public void _a(
      Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
    ActionDefinition actionDef = (ActionDefinition) args.get("arg");
    if (actionDef == null) {
      actionDef = (ActionDefinition) args.get("action");
    }
    if (!("GET".equals(actionDef.method))) {
      String id = uuidGenerator.randomUUID();
      out.print(
          "<form method=\"POST\" id=\""
              + id
              + "\" "
              + (args.containsKey("target") ? "target=\"" + args.get("target") + "\"" : "")
              + " style=\"display:none\" action=\""
              + actionDef.url
              + "\">");
      _authenticityToken(args, body, out, template, fromLine);
      out.print("</form>");
      out.print(
          "<a href=\"javascript:document.getElementById('"
              + id
              + "').submit();\" "
              + serialize(args, "href")
              + ">");
    } else {
      out.print("<a href=\"" + actionDef.url + "\" " + serialize(args, "href") + ">");
    }
    out.print(JavaExtensions.toString(body));
    out.print("</a>");
  }

  public void _error(
      Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
    if (args.get("arg") == null && args.get("key") == null) {
      throw new TemplateException(template.template, fromLine, "Please specify the error key");
    }
    String key = args.get("arg") == null ? args.get("key") + "" : args.get("arg") + "";
    Error error = Validation.error(key);
    if (error != null) {
      if (args.get("field") == null) {
        out.print(error.message());
      } else {
        out.print(error.message(args.get("field") + ""));
      }
    }
  }

  static boolean _evaluateCondition(Object test) {
    if (test != null) {
      if (test instanceof Boolean) {
        return (Boolean) test;
      } else if (test instanceof String) {
        return !((String) test).isEmpty();
      } else if (test instanceof Number) {
        return ((Number) test).intValue() != 0;
      } else if (test instanceof Collection) {
        return !((Collection) test).isEmpty();
      } else return !(test instanceof NullObject);
    }
    return false;
  }

  public static String serialize(Map<?, ?> args, String... unless) {
    StringBuilder attrs = new StringBuilder();
    Arrays.sort(unless);
    for (Object o : args.keySet()) {
      String attr = o.toString();
      String value = args.get(o) == null ? "" : args.get(o).toString();
      if (Arrays.binarySearch(unless, attr) < 0 && !attr.equals("arg")) {
        attrs.append(attr);
        attrs.append("=\"");
        attrs.append(htmlEscape(value));
        attrs.append("\" ");
      }
    }
    return attrs.toString();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public static @interface Namespace {

    String value() default "";
  }
}
