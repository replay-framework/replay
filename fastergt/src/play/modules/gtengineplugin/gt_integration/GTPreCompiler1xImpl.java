package play.modules.gtengineplugin.gt_integration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.template2.GTTemplateRepo;
import play.template2.compile.GTPreCompiler;
import play.template2.legacy.GTLegacyFastTagResolver;

public class GTPreCompiler1xImpl extends GTPreCompiler {
  private final GTLegacyFastTagResolver legacyFastTagResolver = new GTLegacyFastTagResolver1X();

  public GTPreCompiler1xImpl(GTTemplateRepo templateRepo) {
    super(templateRepo, new GTFastTagResolver1x());
  }

  // must modify all use of @{} in tag args
  @Override
  protected String checkAndPatchActionStringsInTagArguments(String tagArgs) {
    // We only have to try to replace the following if we find at least one
    // @ in tagArgs..
    if (tagArgs.indexOf('@') >= 0) {
      tagArgs = tagArgs.replaceAll("[:]\\s*[@]{2}", ":actionBridge._abs().");
      tagArgs = tagArgs.replaceAll("(\\s)[@]{2}", "$1actionBridge._abs().");
      tagArgs = tagArgs.replaceAll("[:]\\s*[@]", ":actionBridge.");
      tagArgs = tagArgs.replaceAll("(\\s)[@]", "$1actionBridge.");
    }
    return tagArgs;
  }

  static final Pattern staticFileP = Pattern.compile("^'(.*)'$");

  @Override
  protected GTFragmentCode generateRegularActionPrinter(
      boolean absolute, String action, SourceContext sc, int lineNo) {

    String code;
    Matcher m = staticFileP.matcher(action.trim());
    if (m.find()) {
      // This is an action/link to a static file.
      action = m.group(1); // without ''
      code = " out.append(__reverseWithCheck_absolute_" + absolute + "(\"" + action + "\"));\n";
    } else {
      if (!action.endsWith(")")) {
        action = action + "()";
      }

      // actionBridge is a special groovy object that will be an object present in the groovy runtime.
      // we must create a groovy method that execute this special groovy actionBridge-hack code, then
      // we return the java code snippet that will get the result from the groovy method, then print the result

      // generate groovy code
      String groovyMethodName = "action_resolver_" + (sc.nextMethodIndex++);

      sc.gprintln(" String " + groovyMethodName + "() {", lineNo);
      if (absolute) {
        sc.gprintln(" return actionBridge._abs()." + action + ";");
      } else {
        sc.gprintln(" return actionBridge." + action + ";");
      }
      sc.gprintln(" }");

      // generate java code that prints it
      code = " out.append(g." + groovyMethodName + "());";
    }

    return new GTFragmentCode(lineNo, code);
  }

  @Override
  public Class<? extends GTGroovyBase> getGroovyBaseClass() {
    return GTGroovyBase1xImpl.class;
  }

  @Override
  public Class<? extends GTJavaBase> getJavaBaseClass() {
    return GTJavaBase1xImpl.class;
  }

  @Override
  public GTLegacyFastTagResolver getGTLegacyFastTagResolver() {
    return legacyFastTagResolver;
  }
}
