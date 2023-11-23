package play.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.TemplateNotFoundException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static org.apache.commons.io.FileUtils.copyURLToFile;

public class TemplateLoader {
    private static final Logger logger = LoggerFactory.getLogger(TemplateLoader.class);

    private static final Map<String, BaseTemplate> templates = new HashMap<>();

    /**
     * Load a template from a file
     * 
     * @param file A File
     * @return The executable template
     */
    public static Template load(File file) {
        return Play.pluginCollection.loadTemplate(file)
          .orElseThrow(() -> new TemplateNotFoundException(file.getAbsolutePath()));
    }

    /**
     * Cleans the cache for all templates
     */
    public static void cleanCompiledCache() {
        templates.clear();
    }

    /**
     * Load a template
     * 
     * @param path
     *            The path of the template (ex: Application/index.html)
     * @return The executable template
     */
    public static Template load(String path) {
        for (File vf : Play.templatesPath) {
            if (vf == null) {
                continue;
            }
            File tf = new File(vf, path);
            boolean templateExists = tf.exists();
            if (!templateExists && Play.usePrecompiled) {
                String name = Play.relativePath(tf).replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_").replace("..", "parent");
                templateExists = Play.getFile("precompiled/templates/" + name).exists();
            }
            if (templateExists) {
                return TemplateLoader.load(tf);
            }
        }

        /*
         * if (template == null) { //When using the old 'key = (file.relativePath().hashCode() + "").replace("-",
         * "M");', //the next line never return anything, since all values written to templates is using the //above
         * key. //when using just file.relativePath() as key, the next line start returning stuff.. //therefor I have
         * commented it out. template = templates.get(path); }
         */
        // TODO: remove ?
        File tf = Play.getVirtualFile(path);
        if (tf != null) {
            return TemplateLoader.load(tf);
        }

        URL fromClasspath = Thread.currentThread().getContextClassLoader().getResource("views/" + path);
        if (fromClasspath != null) {
            return loadTemplateFromClasspath(path, fromClasspath);
        }

        URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
        if (resource != null) {
            return loadTemplateFromClasspath(path, resource);
        }
        
        throw new TemplateNotFoundException(path);
    }

    private static Template loadTemplateFromClasspath(String path, URL resource) {
        File templateFile = new File(Play.tmpDir, path);
        try {
            copyURLToFile(resource, templateFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return TemplateLoader.load(Play.getVirtualFile("tmp/" + path));
    }

    /**
     * List all found templates
     * 
     * @return A list of executable templates
     */
    public static List<Template> getAllTemplate() {
        List<Template> res = new ArrayList<>();
        for (File virtualFile : Play.templatesPath) {
            scan(res, virtualFile);
        }
        return res;
    }

    private static void scan(List<Template> templates, File current) {
        if (!current.isDirectory() && !current.getName().startsWith(".")) {
            long start = nanoTime();
            Template template = load(current);
            if (template != null) {
                template.compile();
                logger.trace("{} ms to load {}", NANOSECONDS.toMillis(nanoTime() - start), current.getName());
                templates.add(template);
            }
        } else if (current.isDirectory() && !current.getName().startsWith(".")) {
            File[] files = current.listFiles();
            if (files != null) {
                for (File virtualFile : files) {
                    scan(templates, virtualFile);
                }
            }
        }
    }
}
