package hello.plugins;

import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

import java.lang.reflect.Method;

public class DebugPlugin extends PlayPlugin {
  @Override
  public void beforeActionInvocation(Request request, Response response, Session session, RenderArgs renderArgs,
                                     Flash flash, Method actionMethod) {
    renderArgs.put("actionName", request.action);
  }
}
