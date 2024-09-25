package play.template2.compile;

import play.template2.GTTemplateRepo;

// Must be implemented by the framework to return the correct impl of the GTPreCompiler
public interface GTPreCompilerFactory {
  GTPreCompiler createCompiler(GTTemplateRepo templateRepo);
}
