package play.templates;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import play.Play;
import play.data.binding.Unbinder;
import play.exceptions.*;
import play.i18n.Messages;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.utils.HTML;
import play.utils.Java;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class ExecutableTemplate extends Script {
    // Leave this field public to allow custom creation of TemplateExecutionException from different pkg
    public BaseTemplate template;

    @Override
    public Object getProperty(String property) {
        try {
            if ("actionBridge".equals(property)) {
                return new ActionBridge(this);
            }
            return super.getProperty(property);
        } catch (MissingPropertyException mpe) {
            return null;
        }
    }

    public void invokeTag(Integer fromLine, String tag, Map<String, Object> attrs, Closure body) {
        String templateName = tag.replace(".", "/");

        BaseTemplate tagTemplate;
        try {
            tagTemplate = (BaseTemplate) TemplateLoader.load("tags/" + templateName + ".tag");
        } catch (TemplateNotFoundException e) {
            try {
                tagTemplate = (BaseTemplate) TemplateLoader.load("tags/" + templateName + ".tag");
            } catch (TemplateNotFoundException ex) {
                throw new TemplateNotFoundException(String.format("tags/%s.tag", templateName), template, fromLine);
            }
        }
        TagContext.enterTag(tag);
        Map<String, Object> args = new HashMap<>();
        args.put("session", getBinding().getVariables().get("session"));
        args.put("flash", getBinding().getVariables().get("flash"));
        args.put("request", getBinding().getVariables().get("request"));
        args.put("params", getBinding().getVariables().get("params"));
        args.put("play", getBinding().getVariables().get("play"));
        args.put("lang", getBinding().getVariables().get("lang"));
        args.put("messages", getBinding().getVariables().get("messages"));
        args.put("out", getBinding().getVariable("out"));
        args.put("_attrs", attrs);
        // all other vars are template-specific
        args.put("_caller", getBinding().getVariables());
        if (attrs != null) {
            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                args.put("_" + entry.getKey(), entry.getValue());
            }
        }
        args.put("_body", body);
        try {
            tagTemplate.internalRender(args);
        } catch (TagInternalException e) {
            throw new TemplateException(template, fromLine, e.getMessage(), e);
        } catch (TemplateNotFoundException e) {
            throw new TemplateNotFoundException(e.getPath(), template, fromLine);
        }
        TagContext.exitTag();
    }

    /**
     * Load the class from Pay Class loader
     *
     * @param className
     *            the class name
     * @return the given class
     */
    public Class __loadClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * This method is faster to call from groovy than __safe() since we only evaluate val.toString() if we need to
     *
     * @param val
     *            the object to evaluate
     * @return The evaluating string
     */
    public String __safeFaster(Object val) {
        if (val instanceof BaseTemplate.RawData) {
            return ((BaseTemplate.RawData) val).data;
        }
        return (val != null) ? val.toString() : "";
    }

    public String __getMessage(Object[] val) {
        if (val == null) {
            throw new NullPointerException("You are trying to resolve a message with an expression " + "that is resolved to null - "
                    + "have you forgotten quotes around the message-key?");
        }
        if (val.length == 1) {
            return Messages.get(val[0]);
        } else {
            // extract args from val
            Object[] args = new Object[val.length - 1];
            for (int i = 1; i < val.length; i++) {
                args[i - 1] = val[i];
            }
            return Messages.get(val[0], args);
        }
    }

    public String __reverseWithCheck_absolute_true(String action) {
        return __reverseWithCheck(action, true);
    }

    public String __reverseWithCheck_absolute_false(String action) {
        return __reverseWithCheck(action, false);
    }

    private String __reverseWithCheck(String action, boolean absolute) {
        return Router.reverseWithCheck(action, Play.file(action), absolute);
    }

    public String __safe(Object val, String stringValue) {
        if (val instanceof BaseTemplate.RawData) {
            return ((BaseTemplate.RawData) val).data;
        }
        if (!template.name.endsWith(".html")) {
            return stringValue;
        }
        return HTML.htmlEscape(stringValue);
    }

    public Object get(String key) {
        return BaseTemplate.layoutData.get().get(key);
    }

    static class ActionBridge extends GroovyObjectSupport {

        ExecutableTemplate template = null;
        String controller = null;
        boolean absolute = false;

        public ActionBridge(ExecutableTemplate template, String controllerPart, boolean absolute) {
            this.template = template;
            this.controller = controllerPart;
            this.absolute = absolute;
        }

        public ActionBridge(ExecutableTemplate template) {
            this.template = template;
        }

        @Override
        public Object getProperty(String property) {
            return new ActionBridge(template, controller == null ? property : controller + "." + property, absolute);
        }

        public Object _abs() {
            this.absolute = true;
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invokeMethod(String name, Object param) {
            try {
                Http.Request request = Http.Request.current();
                if (controller == null) {
                    controller = request.controller;
                }
                String action = controller + "." + name;
                if (action.endsWith(".call")) {
                    action = action.substring(0, action.length() - 5);
                }
                try {
                    Map<String, Object> r = new HashMap<>();
                    Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
                    String[] names = Java.parameterNames(actionMethod);
                    if (param instanceof Object[]) {
                        if (((Object[]) param).length == 1 && ((Object[]) param)[0] instanceof Map) {
                            r = (Map<String, Object>) ((Object[]) param)[0];
                        } else {
                            // too many parameters versus action, possibly a developer error. we must warn him.
                            if (names.length < ((Object[]) param).length) {
                                throw new NoRouteFoundException(action, null);
                            }
                            for (int i = 0; i < ((Object[]) param).length; i++) {
                                if (((Object[]) param)[i] instanceof Router.ActionDefinition && ((Object[]) param)[i] != null) {
                                    Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "",
                                            actionMethod.getAnnotations());
                                } else if (isSimpleParam(actionMethod.getParameterTypes()[i])) {
                                    if (((Object[]) param)[i] != null) {
                                        Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "",
                                                actionMethod.getAnnotations());
                                    }
                                } else {
                                    Unbinder.unBind(r, ((Object[]) param)[i], i < names.length ? names[i] : "",
                                            actionMethod.getAnnotations());
                                }
                            }
                        }
                    }
                    Router.ActionDefinition def = Router.reverse(action, r, request.format, Response.current().encoding);
                    if (absolute) {
                        def.absolute(request);
                    }
                    if (template.template.name.endsWith(".xml")) {
                        def.url = def.url.replace("&", "&amp;");
                    }
                    return def;
                } catch (ActionNotFoundException e) {
                    throw new NoRouteFoundException(action, null);
                }
            } catch (Exception e) {
                if (e instanceof PlayException) {
                    throw (PlayException) e;
                }
                throw new UnexpectedException(e);
            }
        }

        private boolean isSimpleParam(Class type) {
            return Number.class.isAssignableFrom(type) || type.equals(String.class) || type.isPrimitive();
        }
    }
}
