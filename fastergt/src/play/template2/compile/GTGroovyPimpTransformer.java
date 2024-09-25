package play.template2.compile;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ArrayExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import play.template2.GTJavaExtensionsInvoker;

@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GTGroovyPimpTransformer implements ASTTransformation {

  public static GTJavaExtensionMethodResolver gtJavaExtensionMethodResolver;

  static class Trans extends ClassCodeExpressionTransformer {

    final SourceUnit sourceUnit;

    Trans(SourceUnit sourceUnit) {
      this.sourceUnit = sourceUnit;
    }

    @Override
    protected SourceUnit getSourceUnit() {
      return sourceUnit;
    }

    @Override
    public Expression transform(Expression exp) {
      if (exp instanceof MethodCallExpression) {
        MethodCallExpression me = (MethodCallExpression) exp;

        Class jeClazz = gtJavaExtensionMethodResolver.findClassWithMethod(me.getMethodAsString());
        if (jeClazz != null) {
          ClassExpression ce = new ClassExpression(new ClassNode(GTJavaExtensionsInvoker.class));

          ArgumentListExpression newArgs = new ArgumentListExpression();
          ArgumentListExpression args = (ArgumentListExpression) me.getArguments();
          newArgs.getExpressions().add(new ClassExpression(new ClassNode(jeClazz)));
          newArgs.getExpressions().add(new ConstantExpression(me.getMethodAsString()));
          newArgs.getExpressions().add(me.getObjectExpression());
          newArgs
              .getExpressions()
              .add(new ArrayExpression(new ClassNode(Object.class), args.getExpressions()));

          me.setMethod(new ConstantExpression("invoke"));
          me.setArguments(newArgs);
          me.setObjectExpression(ce);
          return exp.transformExpression(this);
        }
      }
      return super.transform(exp);
    }
  }

  @Override
  public void visit(ASTNode[] nodes, SourceUnit source) {
    for (ClassNode classNode : source.getAST().getClasses()) {
      classNode.visitContents(new Trans(source));
    }
  }
}
