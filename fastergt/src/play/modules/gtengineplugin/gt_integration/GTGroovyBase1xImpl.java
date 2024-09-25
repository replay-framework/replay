package play.modules.gtengineplugin.gt_integration;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import play.data.binding.Unbinder;
import play.exceptions.ActionNotFoundException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Router;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.utils.Java;

public class GTGroovyBase1xImpl extends GTGroovyBase {

  @Nullable
  @Override
  public Object getProperty(String property) {
    try {
      if ("actionBridge".equals(property)) {
        // special object used to resolving actions
        GTJavaBase template = (GTJavaBase) super.getProperty("java_class");
        return new ActionBridge(template.templateLocation.relativePath);
      }
      return super.getProperty(property);
    } catch (MissingPropertyException mpe) {
      return null;
    }
  }

  @Override
  public Class _resolveClass(String clazzName) {
    try {
      return Class.forName(clazzName);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  public static class ActionBridge extends GroovyObjectSupport {

    private final String templateName;
    private String controller;
    private boolean absolute;

    public ActionBridge(String templateName, String controllerPart, boolean absolute) {
      this.templateName = templateName;
      this.controller = controllerPart;
      this.absolute = absolute;
    }

    public ActionBridge(String templateName) {
      this.templateName = templateName;
    }

    @Override
    public Object getProperty(String property) {
      return new ActionBridge(
          templateName, controller == null ? property : controller + "." + property, absolute);
    }

    public Object _abs() {
      this.absolute = true;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invokeMethod(String name, Object param) {
      try {
        Http.Request request = Http.Request.current();
        Http.Response response = Http.Response.current();
        if (controller == null) {
          controller = request.controller;
        }
        String action = controller + "." + name;
        if (action.endsWith(".call")) {
          action = action.substring(0, action.length() - 5);
        }
        try {
          Map<String, Object> r = new HashMap<>();
          Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
          String[] names = Java.parameterNames(actionMethod);
          if (param instanceof Object[]) {
            if (((Object[]) param).length == 1 && ((Object[]) param)[0] instanceof Map) {
              r = (Map<String, Object>) ((Object[]) param)[0];
            } else {
              // too many parameters versus action, possibly a developer error. we must warn him.
              if (names.length < ((Object[]) param).length) {
                throw new NoRouteFoundException(action, null);
              }
              for (int i = 0; i < ((Object[]) param).length; i++) {
                if (((Object[]) param)[i] instanceof Router.ActionDefinition
                    && ((Object[]) param)[i] != null) {
                  Unbinder.unBind(
                      r,
                      ((Object[]) param)[i].toString(),
                      i < names.length ? names[i] : "",
                      actionMethod.getAnnotations());
                } else if (isSimpleParam(actionMethod.getParameterTypes()[i])) {
                  if (((Object[]) param)[i] != null) {
                    Unbinder.unBind(
                        r,
                        ((Object[]) param)[i].toString(),
                        i < names.length ? names[i] : "",
                        actionMethod.getAnnotations());
                  }
                } else {
                  Unbinder.unBind(
                      r,
                      ((Object[]) param)[i],
                      i < names.length ? names[i] : "",
                      actionMethod.getAnnotations());
                }
              }
            }
          }
          Router.ActionDefinition def =
              Router.reverse(action, r, request.format, response.encoding);
          if (absolute) {
            def.absolute(request);
          }
          if (templateName.endsWith(".xml")) {
            def.url = def.url.replace("&", "&amp;");
          }
          return def;
        } catch (ActionNotFoundException e) {
          throw new NoRouteFoundException(action, null);
        }
      } catch (PlayException e) {
        throw e;
      } catch (Exception e) {
        throw new UnexpectedException(e);
      }
    }

    static boolean isSimpleParam(Class type) {
      return Number.class.isAssignableFrom(type) || type.equals(String.class) || type.isPrimitive();
    }
  }
}
