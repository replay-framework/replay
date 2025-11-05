package play.mvc;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import play.data.validation.Validation;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;

@NullMarked
@CheckReturnValue
public record ActionContext(Request request, Response response, Session session, Flash flash,
                            RenderArgs renderArgs, Validation validation) {
}
