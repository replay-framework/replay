package plugins;

import jakarta.inject.Inject;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import play.PlayPlugin;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import services.Mathematics;

public class DebugPlugin extends PlayPlugin {
  @Inject private Mathematics mathematics;

  @Override
  public void beforeActionInvocation(
      Request request,
      Response response,
      Session session,
      RenderArgs renderArgs,
      Flash flash,
      Method actionMethod) {
    renderArgs.put("actionName", request.action);
    renderArgs.put("actionMethod", actionMethod.getName());
    renderArgs.put("math", ": SQRT(2)=" + mathematics.sqrt(new BigDecimal(2)));
  }
}
