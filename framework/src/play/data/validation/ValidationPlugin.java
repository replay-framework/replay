package play.data.validation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.guard.Guard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.PlayPlugin;
import play.exceptions.UnexpectedException;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope;
import play.mvc.Scope.RenderArgs;
import play.mvc.Scope.Session;
import play.mvc.results.Result;
import play.utils.ErrorsCookieCrypter;
import play.utils.Java;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static play.data.validation.Error.toValidationError;

public class ValidationPlugin extends PlayPlugin {

    static final ThreadLocal<Map<Object, String>> keys = new ThreadLocal<>();
    private static final ErrorsCookieCrypter errorsCookieCrypter = new ErrorsCookieCrypter();
    private static final Logger securityLogger = LoggerFactory.getLogger("security");
    private static final Gson GSON = new Gson();
    private static final TypeToken<List<Error>> TYPE_ERRORS_LIST = new TypeToken<>() {
    };

    @Override
    public void beforeInvocation() {
        keys.set(new HashMap<>());
        Validation.current.set(new Validation());
    }

    @Override
    public void beforeActionInvocation(Request request, Response response, Session session, RenderArgs renderArgs,
                                       Scope.Flash flash, Method actionMethod) {
        Validation.current.set(restore(request));
        if (!needsValidation(actionMethod)) {
            return;
        }
        List<ConstraintViolation> violations = new Validator().validateAction(request, session, actionMethod);
        List<Error> errors = new ArrayList<>();
        String[] paramNames = Java.parameterNames(actionMethod);
        for (ConstraintViolation violation : violations) {
            String key = paramNames[((MethodParameterContext) violation.getContext()).getParameterIndex()];
            Error error = toValidationError(key, violation);
            errors.add(error);
        }
        Validation.current.get().errors.addAll(errors);
    }

    private boolean needsValidation(Method actionMethod) {
        for (Annotation[] annotations : actionMethod.getParameterAnnotations()) {
            if (annotations.length > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActionInvocationResult(@Nonnull Request request, @Nonnull Response response, @Nonnull Session session, @Nonnull RenderArgs renderArgs, Result result) {
        save(request, response);
    }

    @Override
    public void onActionInvocationException(@Nonnull Http.Request request, @Nonnull Response response, @Nonnull Throwable e) {
        clear(response);
    }

    @Override
    public void onActionInvocationFinally(@Nonnull Request request) {
        onJobInvocationFinally();
    }

    @Override
    public void onJobInvocationFinally() {
        if (keys.get() != null) {
            keys.get().clear();
        }
        keys.remove();
        Validation.current.remove();
    }

    static class Validator extends Guard {
        public List<ConstraintViolation> validateAction(Http.Request request, Session session, Method actionMethod) {
            Object[] rArgs = ActionInvoker.getActionMethodArgs(request, session, actionMethod);

            List<ConstraintViolation> violations = new ArrayList<>();
            violations.addAll(validateMethodParameters(actionMethod, rArgs));
            violations.addAll(validateMethodPre(actionMethod, rArgs));
            return violations;
        }

        private List<ConstraintViolation> validateMethodParameters(Method actionMethod, Object[] rArgs) {
            InternalValidationCycle cycle1 = new InternalValidationCycle(null, null);
            validateMethodParameters(null, actionMethod, rArgs, cycle1);
            return cycle1.violations;
        }

        private List<ConstraintViolation> validateMethodPre(Method actionMethod, Object[] rArgs) {
            InternalValidationCycle cycle2 = new InternalValidationCycle(null, null);
            validateMethodPre(null, actionMethod, rArgs, cycle2);
            return cycle2.violations;
        }
    }

    Validation restore(Request request) {
        try {
            Validation validation = new Validation();
            String cookieName = Scope.COOKIE_PREFIX + "_ERRORS";
            Http.Cookie cookie = request.cookies.get(cookieName);
            if (cookie != null) {
                try {
                    String errorsData = errorsCookieCrypter.decrypt(URLDecoder.decode(cookie.value, UTF_8));
                    List<Error> errors = parseErrorsCookie(errorsData);
                    validation.errors.addAll(errors);
                } catch (RuntimeException e) {
                    securityLogger.error("Failed to decrypt cookie {}={}", cookieName, cookie.value, e);
                }
            }
            return validation;
        } catch (RuntimeException e) {
            securityLogger.error("Failed to restored validation errors from cookie", e);
            return new Validation();
        }
    }

    @Nonnull
    @CheckReturnValue
    List<Error> parseErrorsCookie(String errorsData) {
        try {
            return errorsData == null || errorsData.isEmpty() ? emptyList() : GSON.fromJson(errorsData, TYPE_ERRORS_LIST);
        }
        catch (JsonSyntaxException ignore) {
            return emptyList();
        }
    }

    void save(Request request, Response response) {
        if (response == null) {
            // Some request like WebSocket don't have any response
            return;
        }
        if (Validation.errors().isEmpty()) {
            // Only send "delete cookie" header when the cookie was present in the request
            if (request.cookies.containsKey(Scope.COOKIE_PREFIX + "_ERRORS")) {
                response.setCookie(Scope.COOKIE_PREFIX + "_ERRORS", "", null, "/", 0, Scope.COOKIE_SECURE, Scope.SESSION_HTTPONLY);
            }
            return;
        }
        try {
            String errorsCookieValue = "";
            if (Validation.current() != null && Validation.current().keep) {
                errorsCookieValue = composeErrorsCookieValue(new ArrayList<>(Validation.errors()));
            }
            String errorsData = URLEncoder.encode(errorsCookieCrypter.encrypt(errorsCookieValue), UTF_8);
            response.setCookie(Scope.COOKIE_PREFIX + "_ERRORS", errorsData, null, "/", null, Scope.COOKIE_SECURE, Scope.SESSION_HTTPONLY);
        } catch (Exception e) {
            throw new UnexpectedException("Failed to serialize errors cookie", e);
        }
    }

    @Nonnull
    @CheckReturnValue
    String composeErrorsCookieValue(List<Error> validationErrors) {
        return GSON.toJson(validationErrors);
    }

    private void clear(@Nonnull Response response) {
        try {
            if (response.cookies != null) {
                Cookie cookie = new Cookie(Scope.COOKIE_PREFIX + "_ERRORS", "");
                cookie.sendOnError = true;
                response.cookies.put(cookie.name, cookie);
            }
        } catch (Exception e) {
            throw new UnexpectedException("Errors serializationProblem", e);
        }
    }
}
