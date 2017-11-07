package play.rebel;

import play.mvc.Http;
import play.mvc.Redirector;
import play.mvc.Router;
import play.mvc.results.Redirect;

import java.util.Map;

import static java.util.Arrays.asList;
import static play.mvc.Redirector.toMap;

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

  @Override public void apply(Http.Request request, Http.Response response) {
    this.url = url(action, parameters);
    super.apply(request, response);
  }

  private static String url(String action, Map<String, Object> parameters) {
    if (action.startsWith("@")) {
      action = action.substring(1);
      if (!action.contains(".")) {
        action = Http.Request.current().controller + "." + action;
      }
    }

    Router.ActionDefinition actionDefinition = parameters.isEmpty() ? 
        Router.reverse(action) : Router.reverse(action, parameters);
    return actionDefinition.toString();
  }

  public String getAction() {
    return action;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }
}
