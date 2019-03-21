package play.modules.gtengineplugin;

import groovy.lang.Closure;
import play.templates.ExecutableTemplate;
import play.templates.FastTags;

import java.io.PrintWriter;
import java.util.Map;

/**
 * Fast tags implementation
 */
public class InternalLegacyFastTagsImpls {

    private static FastTags tags = new FastTags();

    // Intentionally not porting fasttags too tied up with the current Play impl

    public static void _jsRoute(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        tags._jsRoute(args, body, out, template, fromLine);
    }

    public static void _authenticityToken(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        tags._authenticityToken(args, body, out, template, fromLine);
    }

    public static void _form(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        tags._form(args, body, out, template, fromLine);
    }

    public static void _field(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        tags._field(args, body, out, template, fromLine);
    }

    public static void _a(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        tags._a(args, body, out, template, fromLine);
    }

    public static void _error(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine) {
        tags._error(args, body, out, template, fromLine);
    }
}
