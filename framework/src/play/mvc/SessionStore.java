package play.mvc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementations of session storage mechanisms.
 */
public interface SessionStore {
    void save(@Nonnull Scope.Session session, @Nonnull Http.Request request, @Nullable Http.Response response);
    Scope.Session restore(@Nonnull Http.Request request);
}
