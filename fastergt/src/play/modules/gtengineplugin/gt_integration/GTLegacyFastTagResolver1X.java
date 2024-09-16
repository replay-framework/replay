package play.modules.gtengineplugin.gt_integration;

import groovy.lang.Closure;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import play.Play;
import play.exceptions.TemplateException;
import play.modules.gtengineplugin.InternalLegacyFastTagsImpls;
import play.template2.GTJavaBase;
import play.template2.exceptions.GTTemplateRuntimeException;
import play.template2.legacy.GTLegacyFastTagResolver;
import play.templates.ExecutableTemplate;
import play.templates.FastTags;

public class GTLegacyFastTagResolver1X implements GTLegacyFastTagResolver {

  private static class LegacyFastTag {
    public final String className;
    public final String methodName;

    private LegacyFastTag(String className, String methodName) {
      this.className = className;
      this.methodName = methodName;
    }
  }

  private static final Object lock = new Object();
  private static Map<String, LegacyFastTag> _tagName2FastTag;

  private static Map<String, LegacyFastTag> getTagName2FastTag() {
    synchronized (lock) {
      if (_tagName2FastTag == null) {
        Map<String, LegacyFastTag> result = new HashMap<>();

        List<Class> classes = new ArrayList<>();
        classes.add(InternalLegacyFastTagsImpls.class);
        classes.addAll(Play.classes.getAssignableClasses(FastTags.class));

        for (Class clazz : classes) {
          FastTags.Namespace namespace =
              (FastTags.Namespace) clazz.getAnnotation(FastTags.Namespace.class);
          String namespacePrefix = "";
          if (namespace != null) {
            namespacePrefix = namespace.value() + ".";
          }
          for (Method m : clazz.getDeclaredMethods()) {

            if (m.getName().startsWith("_") && Modifier.isStatic(m.getModifiers())) {
              String tagName = namespacePrefix + m.getName().substring(1);
              result.put(tagName, new LegacyFastTag(clazz.getName(), m.getName()));
            }
          }
        }
        _tagName2FastTag = result;
      }
      return _tagName2FastTag;
    }
  }

  public static String getFullNameToBridgeMethod() {
    return GTLegacyFastTagResolver1X.class.getName() + ".legacyFastTagBridge";
  }

  @Override
  public LegacyFastTagInfo resolveLegacyFastTag(String tagName) {

    LegacyFastTag tag = getTagName2FastTag().get(tagName);

    if (tag == null) {
      return null;
    }

    return new LegacyFastTagInfo(getFullNameToBridgeMethod(), tag.className, tag.methodName);
  }

  public static void legacyFastTagBridge(
      String legacyFastTagClassName,
      String legacyFastTagMethodName,
      final GTJavaBase gtTemplate,
      Map<String, Object> args,
      Closure body) {
    try {

      // get the class with the fasttag method on
      Class clazz = Class.forName(legacyFastTagClassName);
      // get the method
      Method m =
          clazz.getMethod(
              legacyFastTagMethodName,
              Map.class,
              Closure.class,
              PrintWriter.class,
              ExecutableTemplate.class,
              Integer.TYPE);
      if (!Modifier.isStatic(m.getModifiers())) {
        throw new RuntimeException("A fast-tag method must be static: " + m);
      }

      PrintWriter out = new PrintWriter(gtTemplate.out);
      ExecutableTemplate executableTemplate =
          new ExecutableTemplate() {

            @Override
            public Object run() {
              throw new RuntimeException("Not implemented in this wrapper");
            }

            @Override
            public Object getProperty(String property) {
              return gtTemplate.binding.getProperty(property);
            }
          };

      int fromLine = 0;

      m.invoke(null, args, body, out, executableTemplate, fromLine);
    } catch (InvocationTargetException wrapped) {
      Throwable e = wrapped.getTargetException();
      if (e instanceof TemplateException) {
        // Must be transformed into GTTemplateRuntimeException
        throw new GTTemplateRuntimeException(e.getMessage(), e);
      } else if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      } else if (e instanceof Error) {
        throw (Error) e;
      } else {
        throw new RuntimeException(e);
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Error when executing legacy fastTag "
              + legacyFastTagClassName
              + "."
              + legacyFastTagMethodName,
          e);
    }
  }
}
