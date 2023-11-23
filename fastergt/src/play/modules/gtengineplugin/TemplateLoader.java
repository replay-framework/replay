package play.modules.gtengineplugin;

import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.modules.gtengineplugin.gt_integration.GTFileResolver1xImpl;
import play.modules.gtengineplugin.gt_integration.GTJavaExtensionMethodResolver1x;
import play.modules.gtengineplugin.gt_integration.GTTagContextBridge;
import play.modules.gtengineplugin.gt_integration.PreCompilerFactory;
import play.template2.*;
import play.template2.compile.GTCompiler;
import play.template2.compile.GTGroovyPimpTransformer;
import play.templates.Template;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.net.MalformedURLException;

@ParametersAreNonnullByDefault
public class TemplateLoader {
    private static GTTemplateRepo templateRepo;

    public static void init() {

        // Telling gt-engine that it should use the TagContext-impl in play 1 instead of its own
        GTTagContext.singleton = new GTTagContextBridge();

        GTGroovyPimpTransformer.gtJavaExtensionMethodResolver = new GTJavaExtensionMethodResolver1x();
        // set up folder where we dump generated src
        GTFileResolver.impl = new GTFileResolver1xImpl(Play.templatesPath);


        if ( Play.configuration.getProperty("save-gttemplate-source-to-disk", null) != null ) {
            GTCompiler.srcDestFolder = new File(Play.appRoot, Play.configuration.getProperty("save-gttemplate-source-to-disk", null));
        }

        File folderToDumpClassesIn = null;
        if ( System.getProperty("precompile")!=null) {
            folderToDumpClassesIn = new File(Play.appRoot, "precompiled/java");
        } else if( Play.mode != Play.Mode.PROD ) {
            folderToDumpClassesIn = new File(Play.appRoot, "tmp/gttemplates");
        }

        templateRepo = new GTTemplateRepo(
                Play.mode == Play.Mode.DEV,
                new PreCompilerFactory(),
                Play.usePrecompiled,
                folderToDumpClassesIn);
    }

    /**
     * Load a template from a file
     * @param file A File
     * @return The executable template
     */
    public static Template load(File file) {
        // Use default engine

        GTTemplateLocationReal templateLocation;
        try {
            templateLocation = new GTTemplateLocationReal(Play.relativePath(file), file.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // get it to check and compile it
        GTJavaBase gtJavaBase = getGTTemplateInstance(templateLocation);

        return new GTTemplate(templateLocation, gtJavaBase);

    }

    @Nullable
    protected static GTJavaBase getGTTemplateInstance( GTTemplateLocation templateLocation) {
        return templateRepo.getTemplateInstance( templateLocation );
    }

    /**
     * Load a template from a String
     * @param key A unique identifier for the template, used for retrieving a cached template
     * @param source The template source
     * @return A Template
     */
    public static Template load(String key, String source) {

        GTTemplateLocationWithEmbeddedSource tl = new GTTemplateLocationWithEmbeddedSource(key, source);

        // get it or compile it
        GTJavaBase gtJavaBase = getGTTemplateInstance(tl);

        return new GTTemplate(tl, gtJavaBase);
    }

    /**
     * Load a template
     * @param path The path of the template (ex: Application/index.html)
     * @return The executable template
     */
    public static Template load(String path) {
        Template template = null;
        for (File vf : Play.templatesPath) {
            if (vf == null) {
                continue;
            }
            File tf = new File(vf, path);
            if (tf.exists()) {
                template = load(tf);
                break;
            }
        }

        if (template == null) {
            File tf = Play.getVirtualFile(path);
            if (tf != null && tf.exists()) {
                template = load(tf);
            } else {
                throw new TemplateNotFoundException(path);
            }
        }
        return template;
    }
}
