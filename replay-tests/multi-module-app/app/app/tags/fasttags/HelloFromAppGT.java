package tags.fasttags;

import play.template2.GTContentRenderer;
import play.template2.GTFastTag;
import play.template2.GTJavaBase;
import java.util.Map;

@GTFastTag.TagNamespace("appGT")
public class HelloFromAppGT extends GTFastTag {
  public static void tag_hello (GTJavaBase template, Map<String, Object> args, GTContentRenderer content) {
    String name = args.containsKey("name") ? args.get("name").toString() : "";
    if (name.isEmpty()) {
      throw new play.exceptions.TagInternalException("name attribute cannot be empty for " + Thread.currentThread().getStackTrace()[1].getMethodName().substring(4) + " tag");
    }

    template.out.append("Hello from ")
        .append(HelloFromAppGT.class.getAnnotation(GTFastTag.TagNamespace.class).value())
        .append(", ").append(name).append("!");
  }
}
