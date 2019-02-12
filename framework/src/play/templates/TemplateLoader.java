package play.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.io.FileUtils.copyURLToFile;

public class TemplateLoader {
    private static final Logger logger = LoggerFactory.getLogger(TemplateLoader.class);

    private static final Map<String, BaseTemplate> templates = new HashMap<>();

    /**
     * Load a template from a virtual file
     * 
     * @param file
     *            A VirtualFile
     * @return The executable template
     */
    public static Template load(VirtualFile file) {
        return Play.pluginCollection.loadTemplate(file)
          .orElseThrow(() -> new TemplateNotFoundException(file.relativePath()));
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
        Template template = null;
        for (VirtualFile vf : Play.templatesPath) {
            if (vf == null) {
                continue;
            }
            VirtualFile tf = vf.child(path);
            boolean templateExists = tf.exists();
            if (!templateExists && Play.usePrecompiled) {
                String name = tf.relativePath().replaceAll("\\{(.*)\\}", "from_$1").replace(":", "_").replace("..", "parent");
                templateExists = Play.getFile("precompiled/templates/" + name).exists();
            }
            if (templateExists) {
                template = TemplateLoader.load(tf);
                break;
            }
        }
        /*
         * if (template == null) { //When using the old 'key = (file.relativePath().hashCode() + "").replace("-",
         * "M");', //the next line never return anything, since all values written to templates is using the //above
         * key. //when using just file.relativePath() as key, the next line start returning stuff.. //therefor I have
         * commented it out. template = templates.get(path); }
         */
        // TODO: remove ?
        if (template == null) {
            VirtualFile tf = Play.getVirtualFile(path);
            if (tf != null && tf.exists()) {
                template = TemplateLoader.load(tf);
            }
        }
        
        if (template == null) {
            URL resource = Thread.currentThread().getContextClassLoader().getResource(path);
            if (resource != null) {
                File tmpTemplateFile = new File(Play.tmpDir, path);
                try {
                    copyURLToFile(resource, tmpTemplateFile);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
                template = TemplateLoader.load(Play.getVirtualFile("tmp/" + path));
            }
        }

        if (template == null) {
            throw new TemplateNotFoundException(path);
        }

        return template;
    }

    /**
     * List all found templates
     * 
     * @return A list of executable templates
     */
    public static List<Template> getAllTemplate() {
        List<Template> res = new ArrayList<>();
        for (VirtualFile virtualFile : Play.templatesPath) {
            scan(res, virtualFile);
        }
        return res;
    }

    private static void scan(List<Template> templates, VirtualFile current) {
        if (!current.isDirectory() && !current.getName().startsWith(".")) {
            long start = System.currentTimeMillis();
            Template template = load(current);
            if (template != null) {
                template.compile();
                logger.trace("{}ms to load {}", System.currentTimeMillis() - start, current.getName());
                templates.add(template);
            }
        } else if (current.isDirectory() && !current.getName().startsWith(".")) {
            for (VirtualFile virtualFile : current.list()) {
                scan(templates, virtualFile);
            }
        }
    }
}
