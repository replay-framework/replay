package play.modules.gtengineplugin;

import groovy.lang.Closure;
import play.templates.FastTags;
import play.templates.GroovyTemplate.ExecutableTemplate;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Fast tags implementation
 */
public class InternalLegacyFastTagsImpls {

    // Intentionally not porting fasttags too tied up with the current Play impl

    public static void _jsRoute(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        FastTags._jsRoute(args, body, out, template, fromLine);
    }

    public static void _authenticityToken(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        FastTags._authenticityToken(args, body, out, template, fromLine);
    }

    public static void _form(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        FastTags._form(args, body, out, template, fromLine);
    }

    public static void _field(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        FastTags._field(args, body, out, template, fromLine);
    }

    public static void _a(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        FastTags._a(args, body, out, template, fromLine);
    }

    public static void _error(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        FastTags._error(args, body, out, template, fromLine);
    }
}
