package play.rebel;

import static java.util.Arrays.asList;
import static play.mvc.Redirector.toMap;

import java.util.Map;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Redirector;
import play.mvc.Router;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Redirect;

public class RedirectToAction extends Redirect {
  private final String action;
  private final Map<String, Object> parameters;

  public RedirectToAction(String action, Map<String, Object> parameters) {
    super("not defined yet");
    this.action = action;
    this.parameters = parameters;
  }

  public RedirectToAction(String action, Redirector.Parameter... args) {
    this(action, toMap(asList(args)));
  }

  @Override
  public void apply(
      Request request, Response response, Session session, RenderArgs renderArgs, Flash flash) {
    this.url = url(request, action, parameters);
    super.apply(request, response, session, renderArgs, flash);
  }

  private static String url(Request request, String action, Map<String, Object> parameters) {
    if (action.startsWith("@")) {
      action = action.substring(1);
      if (!action.contains(".")) {
        action = request.controller + "." + action;
      }
    }

    Router.ActionDefinition actionDefinition =
        parameters.isEmpty() ? Router.reverse(action) : Router.reverse(action, parameters);
    return actionDefinition.toString();
  }

  public String getAction() {
    return action;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }
}
