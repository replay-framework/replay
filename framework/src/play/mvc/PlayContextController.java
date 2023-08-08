package play.mvc;

/**
 * Interface for play controllers with ActionContext provided to them by the {@link play.mvc.ActionInvoker}.
 * 
 * This class your controllers should implement, when they need access to the {@link play.mvc.ActionContext}.
 * In most cases, you can extend {@link play.mvc.Controller} which provides a basic set of features
 * useful when implementing controllers.
 */
public interface PlayContextController extends PlayController {
  void setContext(ActionContext actionContext);
}
