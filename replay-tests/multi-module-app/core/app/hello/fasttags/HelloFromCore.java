package hello.fasttags;

import groovy.lang.Closure;
import play.templates.ExecutableTemplate;
import play.templates.FastTags;
import java.io.PrintWriter;
import java.util.Map;

@FastTags.Namespace("core")
public class HelloFromCore extends FastTags {

  public static void _hello (Map<?, ?> args, Closure<String> body, PrintWriter out, ExecutableTemplate template, int fromLine) {
    String name = args.containsKey("name") ? args.get("name").toString() : "";
    if (name.isEmpty()) {
      throw new play.exceptions.TagInternalException("name attribute cannot be empty for " + Thread.currentThread().getStackTrace()[1].getMethodName().substring(1) + " tag");
    }
    out.print("Hello from " + HelloFromCore.class.getAnnotation(FastTags.Namespace.class).value() + ", " + name + "!");
  }
}
