package play.template2;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public abstract class GTFastTag implements GTFastTagResolver {

  // We need a default constructor so we can new() it when resolving tags.
  protected GTFastTag() {}

  @Override
  public String resolveFastTag(String tagName) {

    // Check this class is annotated with @TagNamespace
    TagNamespace tagNamespace = getClass().getAnnotation(TagNamespace.class);
    if (tagNamespace != null) {
      String namespace = tagNamespace.value();
      // Check if tagName starts with this namespace.
      if (!tagName.startsWith(namespace + ".")) {
        // Namespace does not match.
        return null;
      }
      // Remove namespace from tagName before we look for tag-method.
      tagName = tagName.substring(namespace.length() + 1);
    }

    // Look for static methods in this class with the name "tag_tagName".
    try {
      Method m =
          getClass()
              .getMethod("tag_" + tagName, GTJavaBase.class, Map.class, GTContentRenderer.class);
      if (!Modifier.isStatic(m.getModifiers())) {
        throw new RuntimeException("A fast-tag method must be static: " + m);
      }
    } catch (NoSuchMethodException e) {
      // Not found.
      return null;
    }

    return getClass().getName() + ".tag_" + tagName;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.TYPE)
  public @interface TagNamespace {
    String value() default "";
  }
}
