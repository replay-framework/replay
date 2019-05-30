package play.template2.compile;

import play.Play;
import play.template2.*;
import play.template2.exceptions.GTTemplateRuntimeException;
import play.template2.legacy.GTContentRendererFakeClosure;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.binarySearch;
import static play.utils.HTML.htmlEscape;

public class GTInternalFastTags extends GTFastTag {



    public static void tag_get(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {

        String key = args.get("arg").toString();
        if ( key == null) {
            throw new GTTemplateRuntimeException("Specify a variable name when using #{get/}");
        }

        Object value = GTJavaBase.layoutData.get().get(key);

        if (value != null) {
            template.out.append(value.toString());
        } else {
            if ( content != null ) {
                template.insertOutput( content.render() );
            }
        }

    }

    public static void tag_set(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        String key = null;
        Object value = null;
        // Simple case : #{set title:'Yop' /}

        for ( String k : args.keySet()) {
            if ( !"arg".equals(k)) {
                key = k;
                Object v = args.get(key);
                
                if ( v instanceof String) {
                    value = template.objectToString( v);
                } else {
                    value = v;
                }


                break;
            }
        }

        if ( key == null) {
            // Body case
            key = args.get("arg").toString();
            // render content to string
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Charset encoding = (Charset) content.getRuntimeProperty("_response_encoding");
            if ( encoding == null ) {
                encoding = Play.defaultWebEncoding;
            }
            content.render().writeOutput(out, encoding);
            value = out.toString(encoding);
        }

        if ( key != null ) {
            GTJavaBase.layoutData.get().put(key, value);
        }
    }

    public static void tag_ifErrors(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        if ( template.validationHasErrors()) {
            template.clearElseFlag();
            template.insertOutput( content.render());
        } else {
            // Must set the else-condition
            template.setElseFlag();
        }
    }

    public static void tag_ifError(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        Object key = args.get("arg");
        if (key==null) {
            throw new GTTemplateRuntimeException("Please specify the error key");
        }
        if ( template.validationHasError(key.toString())) {
            template.clearElseFlag();
            template.insertOutput( content.render());
        } else {
            // Must set the else-condition
            template.setElseFlag();
        }
    }


    public static void tag_include(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        if (!args.containsKey("arg") || args.get("arg") == null) {
            throw new GTTemplateRuntimeException("Specify a template name");
        }
        String name = args.get("arg").toString();
        GTTemplateLocationReal templateLocation = template.resolveTemplateLocation( name );

        if ( templateLocation == null) {
            throw new GTTemplateRuntimeException("Cannot find template");
        }

        GTJavaBase newTemplate = template.templateRepo.getTemplateInstance( templateLocation );
        Map<String, Object> newArgs = new HashMap<String, Object>();
        newArgs.putAll(template.binding.getVariables());
        newArgs.put("_isInclude", true);

        newTemplate.internalRenderTemplate(newArgs, false, template);
        template.insertOutput( newTemplate );
    }

    public static void tag_render(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        if (!args.containsKey("arg") || args.get("arg") == null) {
            throw new GTTemplateRuntimeException("Specify a template name");
        }
        String name = args.get("arg").toString();
        GTTemplateLocationReal templateLocation = template.resolveTemplateLocation( name );

        if ( templateLocation == null) {
            throw new GTTemplateRuntimeException("Cannot find template");
        }

        GTJavaBase newTemplate = template.templateRepo.getTemplateInstance( templateLocation );
        Map<String, Object> newArgs = new HashMap<>();
        newArgs.putAll(args);
        newArgs.put("_isInclude", true);

        newTemplate.internalRenderTemplate(newArgs, false, template);
        template.insertOutput( newTemplate );
    }

    public static void tag_doBody(GTJavaBase template, Map<String, Object> args, GTContentRenderer _content ) {

        GTContentRenderer body = template.contentRenderer;
        // have someone modified which body we should render?
        GTContentRendererFakeClosure bodyClosure = (GTContentRendererFakeClosure)args.get("body");
        if ( bodyClosure != null) {
            body = bodyClosure.contentRenderer;
        }


        // the content we're supposed to output here is the body-content inside the tag we're now in.
        // we must not output the body of the doBody-tag it self.
        // output this: template.contentRenderer


        // if we have an arg named "vars" which is a map, then
        // we should inject the key->values in var into args to body.
        // if the org value of the key, is null, we should restore the value after we have rendered.
        Map<String, Object> vars = (Map<String, Object>)args.get("vars");

        Set<String> propertiesToResetToNull = new HashSet<>();

        if ( vars != null) {
            for (Map.Entry<String, Object> e : vars.entrySet()) {
                String key = e.getKey();
                if ( body.getRuntimeProperty(key) == null ) {
                    // this one should reseted after rendering
                    propertiesToResetToNull.add( key);
                }
                // set the value
                body.setRuntimeProperty(key, e.getValue());
            }
        }

        String as = (String)args.get("as");


        if ( as == null ) {
            // render body right now
            template.insertOutput(body.render());
        } else {
            // render body to string and store it with the name in as
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            body.render().writeOutput(out, UTF_8);
            body.setRuntimeProperty(as, out.toString(UTF_8));

        }

        // do we have anything to reset?
        for ( String key : propertiesToResetToNull) {
            body.setRuntimeProperty(key, null);
        }

    }

    public static void tag_cache(GTJavaBase template, Map<String, Object> args, GTContentRenderer _content ) {
        String key = args.get("arg").toString();
        String duration = null;
        if (args.containsKey("for")) {
            duration = args.get("for").toString();
        }
        Object cached = template.cacheGet(key);
        if (cached != null) {
            template.out.append(cached.toString());
            return;
        }
        GTRenderingResult renderingResult = _content.render();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderingResult.writeOutput(out, UTF_8);
        String result = new String(out.toByteArray(), UTF_8);
        template.cacheSet(key, result, duration);
        template.out.append(result);
    }

    public static void tag_jsAction(GTJavaBase template, Map<String, Object> args, GTContentRenderer _content ) {
        template.out.append("function(options) {var pattern = '" + args.get("arg").toString().replace("&amp;", "&") + "'; for(key in options) { pattern = pattern.replace(':'+key, options[key]); } return pattern }");
    }

    public static void tag_option(GTJavaBase template, Map<String, Object> args, GTContentRenderer _content ) {
        Object value = args.get("arg");
        Object selectedValue = GTTagContext.singleton.parent("select").getData().get("selected");
        boolean selected = selectedValue != null && value != null && (selectedValue.toString()).equals(value.toString());
        template.out.append("<option value=\"")
            .append(htmlEscape(String.valueOf(value == null ? "" : value)))
            .append("\" ")
            .append(selected ? "selected" : "")
            .append(" ")
            .append(serialize(args, "selected", "value"))
            .append(">");
        template.insertOutput(_content.render());
        template.out.append("</option>");
    }

    public static void tag_errorClass(GTJavaBase template, Map<String, Object> args, GTContentRenderer _content ) {
        if (args.get("arg") == null) {
            throw new GTTemplateRuntimeException("Please specify the error key");
        }
        if (template.validationHasError(args.get("arg").toString())) {
            template.out.append("hasError");
        }
    }

    public static String serialize(Map<?, ?> args, String... unless) {
        StringBuilder attrs = new StringBuilder();
        Arrays.sort(unless);
        for (Object o : args.keySet()) {
            String attr = o.toString();
            String value = args.get(o) == null ? "" : args.get(o).toString();
            if (binarySearch(unless, attr) < 0 && !"arg".equals(attr)) {
                attrs.append(attr);
                attrs.append("=\"");
                attrs.append(htmlEscape(value));
                attrs.append("\" ");
            }
        }
        return attrs.toString();
    }


    public static void tag_secureInlineJavaScript(GTJavaBase template, Map<String, Object> args, GTContentRenderer content) {
        content.setRuntimeProperty("__inside_script_tag", "true");

        try {
            template.insertOutput( content.render() );
        }
        finally {
            content.setRuntimeProperty("__inside_script_tag", "false");
        }
    }
}
