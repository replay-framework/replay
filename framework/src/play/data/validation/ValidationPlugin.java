package play.data.validation;

import net.sf.oval.ConstraintViolation;
import net.sf.oval.context.MethodParameterContext;
import net.sf.oval.guard.Guard;
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
import play.utils.Java;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationPlugin extends PlayPlugin {

    public static final ThreadLocal<Map<Object, String>> keys = new ThreadLocal<>();

    @Override
    public void beforeInvocation() {
        keys.set(new HashMap<>());
        Validation.current.set(new Validation());
    }

    @Override
    public void beforeActionInvocation(Request request, Response response, Session session, RenderArgs renderArgs, Method actionMethod) {
        try {
            Validation.current.set(restore());
            boolean verify = false;
            for (Annotation[] annotations : actionMethod.getParameterAnnotations()) {
                if (annotations.length > 0) {
                    verify = true;
                    break;
                }
            }
            if (!verify) {
                return;
            }
            List<ConstraintViolation> violations = new Validator().validateAction(request, actionMethod);
            ArrayList<Error> errors = new ArrayList<>();
            String[] paramNames = Java.parameterNames(actionMethod);
            for (ConstraintViolation violation : violations) {
                errors.add(new Error(
                        paramNames[((MethodParameterContext) violation
                                .getContext()).getParameterIndex()], violation
                                .getMessage(),
                        violation.getMessageVariables() == null ? new String[0]
                                : violation.getMessageVariables().values()
                                        .toArray(new String[0]), violation
                                .getSeverity()));
            }
            Validation.current.get().errors.addAll(errors);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public void onActionInvocationResult(Result result) {
        save();
    }

    @Override
    public void onInvocationException(Throwable e) {
        clear();
    }

    @Override
    public void invocationFinally() {
        if (keys.get() != null) {
            keys.get().clear();
        }
        keys.remove();
        Validation.current.remove();
    }

    // ~~~~~~
    static class Validator extends Guard {
        public List<ConstraintViolation> validateAction(Http.Request request, Method actionMethod) throws Exception {
            List<ConstraintViolation> violations = new ArrayList<>();
            Object instance = null;
            Object[] rArgs = ActionInvoker.getActionMethodArgs(request, actionMethod);
            validateMethodParameters(null, actionMethod, rArgs, violations);
            validateMethodPre(null, actionMethod, rArgs, violations);
            return violations;
        }
    }
    static Pattern errorsParser = Pattern.compile("\u0000([^:]*):([^\u0000]*)\u0000");

    static Validation restore() {
        try {
            Validation validation = new Validation();
            Http.Cookie cookie = Request.current().cookies.get(Scope.COOKIE_PREFIX + "_ERRORS");
            if (cookie != null) {
                String errorsData = URLDecoder.decode(cookie.value, "utf-8");
                Matcher matcher = errorsParser.matcher(errorsData);
                while (matcher.find()) {
                    String[] g2 = matcher.group(2).split("\u0001", -1);
                    String message = g2[0];
                    String[] args = new String[g2.length - 1];
                    System.arraycopy(g2, 1, args, 0, args.length);
                    validation.errors.add(new Error(matcher.group(1), message, args));
                }
            }
            return validation;
        } catch (Exception e) {
            return new Validation();
        }
    }

    static void save() {
        if (Response.current() == null) {
            // Some request like WebSocket don't have any response
            return;
        }
        if (Validation.errors().isEmpty()) {
            // Only send "delete cookie" header when the cookie was present in the request
            if(Request.current().cookies.containsKey(Scope.COOKIE_PREFIX + "_ERRORS") || !Scope.SESSION_SEND_ONLY_IF_CHANGED) {
                Response.current().setCookie(Scope.COOKIE_PREFIX + "_ERRORS", "", null, "/", 0, Scope.COOKIE_SECURE, Scope.SESSION_HTTPONLY);
            }
            return;
        }
        try {
            StringBuilder errors = new StringBuilder();
            if (Validation.current() != null && Validation.current().keep) {
                for (Error error : Validation.errors()) {
                    errors.append("\u0000");
                    errors.append(error.key);
                    errors.append(":");
                    errors.append(error.message);
                    for (String variable : error.variables) {
                        errors.append("\u0001");
                        errors.append(variable);
                    }
                    errors.append("\u0000");
                }
            }
            String errorsData = URLEncoder.encode(errors.toString(), "utf-8");
            Response.current().setCookie(Scope.COOKIE_PREFIX + "_ERRORS", errorsData, null, "/", null, Scope.COOKIE_SECURE, Scope.SESSION_HTTPONLY);
        } catch (Exception e) {
            throw new UnexpectedException("Errors serializationProblem", e);
        }
    }

    static void clear() {
        try {
            if (Response.current() != null && Response.current().cookies != null) {
                Cookie cookie = new Cookie();
                cookie.name = Scope.COOKIE_PREFIX + "_ERRORS";
                cookie.value = "";
                cookie.sendOnError = true;
                Response.current().cookies.put(cookie.name, cookie);
            }
        } catch (Exception e) {
            throw new UnexpectedException("Errors serializationProblem", e);
        }
    }
}
