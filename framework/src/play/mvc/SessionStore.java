package play.mvc;

/**
 * Implementations of session storage mechanisms.
 */
public interface SessionStore {
    void save(Scope.Session session, Http.Request request, Http.Response response);
    Scope.Session restore(Http.Request request);
}
