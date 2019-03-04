package play.mvc;

import play.data.validation.Validation;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

import javax.annotation.Nonnull;

public class ActionContext {
  @Nonnull public final Request request;
  @Nonnull public final Response response;
  @Nonnull public final Session session;
  @Nonnull public final Flash flash;
  @Nonnull public final RenderArgs renderArgs;
  @Nonnull public final Validation validation;

  public ActionContext(@Nonnull Request request, @Nonnull Response response,
                       @Nonnull Session session, @Nonnull Flash flash,
                       @Nonnull RenderArgs renderArgs,
                       @Nonnull Validation validation) {
    this.request = request;
    this.response = response;
    this.session = session;
    this.flash = flash;
    this.renderArgs = renderArgs;
    this.validation = validation;
  }
}
