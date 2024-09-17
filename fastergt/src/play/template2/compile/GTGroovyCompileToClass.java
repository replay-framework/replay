package play.template2.compile;

import groovy.lang.GroovyClassLoader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilationUnit.GroovyClassOperation;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.Message;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;
import org.codehaus.groovy.tools.GroovyClass;
import play.template2.GTLineMapper;
import play.template2.GTTemplateLocation;
import play.template2.exceptions.GTCompilationException;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;

public class GTGroovyCompileToClass {

  private final ClassLoader parentClassLoader = Thread.currentThread().getContextClassLoader();

  static class GTCompilationUnit extends CompilationUnit {
    GTCompilationUnit(CompilerConfiguration configuration) {
      super(
          configuration,
          null,
          null,
          new GroovyClassLoader(GTCompilationUnit.class.getClassLoader()));
      //optimizer = new GTOptimizerVisitor(this);
    }

    public Collection<GroovyClassOperation>[] getPhases() {
      try {
        Field phasesF = CompilationUnit.class.getDeclaredField("phaseOperations");
        phasesF.setAccessible(true);
        //noinspection unchecked
        return (Collection<GroovyClassOperation>[]) phasesF.get(this);
      } catch (Exception e) {
        throw new RuntimeException("Not supposed to happen", e);
      }
    }
  }

  public GTJavaCompileToClass.CompiledClass[] compileGroovySource(
      GTTemplateLocation templateLocation, GTLineMapper lineMapper, String groovySource) {
    try (GroovyClassLoader classLoader = new GroovyClassLoader(parentClassLoader)) {
      CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
      compilerConfiguration.setSourceEncoding("utf-8");
      GTCompilationUnit compilationUnit = new GTCompilationUnit(compilerConfiguration);
      compilationUnit.addSource(
          new SourceUnit(
              "",
              groovySource,
              compilerConfiguration,
              classLoader,
              compilationUnit.getErrorCollector()));
      Collection<GroovyClassOperation>[] phases = compilationUnit.getPhases();

      LinkedList<GroovyClassOperation> output = new LinkedList<>();
      phases[Phases.OUTPUT] = output;
      final List<GroovyClass> groovyClassesForThisTemplate = new ArrayList<>();
      output.add(
          new GroovyClassOperation() {
            @Override
            public void call(GroovyClass groovyClass) {
              groovyClassesForThisTemplate.add(groovyClass);
            }
          });

      try {
        compilationUnit.compile();
      } catch (MultipleCompilationErrorsException e) {
        if (e.getErrorCollector().getErrorCount() == 0) {
          throw new GTCompilationException("Error compiling groovy", e);
        }

        Message errorMessage = e.getErrorCollector().getError(0);
        if (errorMessage instanceof SyntaxErrorMessage) {
          SyntaxException se = ((SyntaxErrorMessage) errorMessage).getCause();
          throw new GTCompilationExceptionWithSourceInfo(
              se.getOriginalMessage(), templateLocation, lineMapper.translateLineNo(se.getLine()));
        }

        throw new GTCompilationException("Error compiling groovy", e);
      }

      GTJavaCompileToClass.CompiledClass[] result =
          new GTJavaCompileToClass.CompiledClass[groovyClassesForThisTemplate.size()];
      for (int i = 0; i < result.length; i++) {
        GroovyClass groovyClass = groovyClassesForThisTemplate.get(i);
        result[i] =
            new GTJavaCompileToClass.CompiledClass(groovyClass.getName(), groovyClass.getBytes());
      }

      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
