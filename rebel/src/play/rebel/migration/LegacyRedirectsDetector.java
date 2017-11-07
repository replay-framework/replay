package play.rebel.migration;

import javassist.*;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.annotation.Annotation;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.classloading.enhancers.ControllersEnhancer;
import play.classloading.enhancers.Enhancer;

import java.util.HashSet;
import java.util.Set;

public class LegacyRedirectsDetector extends PlayPlugin {
  private static final Logger logger = LoggerFactory.getLogger(LegacyRedirectsDetector.class);
  
  private static final Set<String> calledMethods = new HashSet<>();

  @Override public void afterApplicationStart() {
    if (calledMethods.isEmpty()) {
      logger.info("Congratulations! No legacy redirects are used in your project.");
      return;
    }
    
    StringBuilder sb = new StringBuilder();
    sb.append("Legacy redirects found!\n");
    sb.append("The following method calls are converted to a redirect by ControllerEhnancer:\n");
    for (String calledMethod : calledMethods) {
      sb.append(calledMethod).append("\n");
    }
    sb.append("\n");
    logger.warn(sb.toString());
  }

  @Override public void enhance(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
    new LegacyRedirectUsageDetectorEnhancer().enhanceThisClass(applicationClass);
  }
  
  private static class LegacyRedirectUsageDetectorEnhancer extends Enhancer {
    CtClass controllerSupportClasss = classPool.get(ControllersEnhancer.ControllerSupport.class.getName());

    private LegacyRedirectUsageDetectorEnhancer() throws NotFoundException {
    }

    @Override public void enhanceThisClass(ApplicationClasses.ApplicationClass applicationClass) throws Exception {
      CtClass ctClass = makeClass(applicationClass);
      for (final CtMethod ctMethod : ctClass.getDeclaredMethods()) {
        interceptAllMethodAccess(ctClass, ctMethod);
      }
    }
    private void interceptAllMethodAccess(CtClass ctClass, final CtMethod ctMethod) throws CannotCompileException {
      ctMethod.instrument(new ExprEditor() {
        @Override public void edit(MethodCall m) {
          try {
            if (isActionMethod(m.getMethod())) {
              calledMethods.add(m.getClassName() + '.' + m.getMethodName());
            }
          }
          catch (NotFoundException e) {
            // log it: javassist.NotFoundException: access$1(..) is not found in integrations.PreloadingScheduler
          }
        }
      });
    }

    private boolean isActionMethod(CtMethod ctMethod) throws NotFoundException {
      return ctMethod.getDeclaringClass().subtypeOf(controllerSupportClasss)
          && Modifier.isPublic(ctMethod.getModifiers())
          && Modifier.isStatic(ctMethod.getModifiers())
          && ctMethod.getReturnType().equals(CtClass.voidType)
          && !isHandler(ctMethod);
    }

    private boolean isHandler(CtMethod ctMethod) {
      for (Annotation a : getAnnotations(ctMethod).getAnnotations()) {
        if (a.getTypeName().startsWith("play.mvc.")) {
          return true;
        }
        if (a.getTypeName().endsWith("$ByPass")) {
          return true;
        }
      }
      return false;
    }

    protected static AnnotationsAttribute getAnnotations(CtMethod ctMethod) {
      AnnotationsAttribute annotationsAttribute = (AnnotationsAttribute) ctMethod.getMethodInfo().getAttribute(AnnotationsAttribute.visibleTag);
      if (annotationsAttribute == null) {
        annotationsAttribute = new AnnotationsAttribute(ctMethod.getMethodInfo().getConstPool(), AnnotationsAttribute.visibleTag);
        ctMethod.getMethodInfo().addAttribute(annotationsAttribute);
      }
      return annotationsAttribute;
    }

  }
}
