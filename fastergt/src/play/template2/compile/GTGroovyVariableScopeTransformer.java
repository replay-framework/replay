package play.template2.compile;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeExpressionTransformer;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

@GroovyASTTransformation(phase = CompilePhase.CONVERSION)
public class GTGroovyVariableScopeTransformer implements ASTTransformation {

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

      if (exp instanceof DeclarationExpression) {
        // info: http://stackoverflow.com/questions/8158637/using-asttransformation-to-remove-def-to-change-scope-in-groovy-script
        // DeclarationExpression is used when something is declared like this:
        // def a = "hi";
        // Long b = 1;
        //
        // When we change it to use BinaryExpression, we "remove" the def or Long, making it "behave" like this:
        // a = "hi";
        // b = 1;
        // This changes the scope

        BinaryExpression e =
            new BinaryExpression(
                ((DeclarationExpression) exp).getLeftExpression(),
                ((DeclarationExpression) exp).getOperation(),
                ((DeclarationExpression) exp).getRightExpression());
        // setting source position
        e.setSourcePosition(exp);
        return e.transformExpression(this);
      } else {
        return super.transform(exp);
      }
    }
  }

  @Override
  public void visit(ASTNode[] nodes, SourceUnit source) {
    for (ClassNode classNode : source.getAST().getClasses()) {
      classNode.visitContents(new Trans(source));
    }
  }
}
