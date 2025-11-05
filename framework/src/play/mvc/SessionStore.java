package play.mvc;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implementations of session storage mechanisms.
 */
@NullMarked
@CheckReturnValue
public interface SessionStore {

  void save(
      Scope.Session session,
      Http.Request request,
      Http.@Nullable Response response);

  Scope.Session restore(Http.Request request);
}
