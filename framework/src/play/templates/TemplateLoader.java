package play.templates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.exceptions.TemplateNotFoundException;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.io.FileUtils.copyURLToFile;

public class TemplateLoader {
    private static final Logger logger = LoggerFactory.getLogger(TemplateLoader.class);

    protected static Map<String, BaseTemplate> templates = new HashMap<>();
    /**
     * See getUniqueNumberForTemplateFile() for more info
     */
    private static AtomicLong nextUniqueNumber = new AtomicLong(1000);// we start on 1000
    private static Map<String, String> templateFile2UniqueNumber = Collections.synchronizedMap(new HashMap<String, String>());

    /**
     * All loaded templates is cached in the templates-list using a key. This key is included as part of the classname
     * for the generated class for a specific template. The key is included in the classname to make it possible to
     * resolve the original template-file from the classname, when creating cleanStackTrace
     *
     * This method returns a unique representation of the path which is usable as part of a classname
     *
     * @param path
     *            Path of the template file
     * @return a unique representation of the path which is usable as part of a classname
     */
    public static String getUniqueNumberForTemplateFile(String path) {
        // a path cannot be a valid classname so we have to convert it somehow.
        // If we did some encoding on the path, the result would be at least as long as the path.
        // Therefor we assign a unique number to each path the first time we see it, and store it..
        // This way, all seen paths gets a unique number. This number is our UniqueValidClassnamePart..

        String uniqueNumber = templateFile2UniqueNumber.get(path);
        if (uniqueNumber == null) {
            // this is the first time we see this path - must assign a unique number to it.
            uniqueNumber = Long.toString(nextUniqueNumber.getAndIncrement());
            templateFile2UniqueNumber.put(path, uniqueNumber);
        }
        return uniqueNumber;
    }

    /**
     * Load a template from a virtual file
     * 
     * @param file
     *            A VirtualFile
     * @return The executable template
     */
    public static Template load(VirtualFile file) {
        Template template = Play.pluginCollection.loadTemplate(file);
        if (template == null)
            throw new TemplateNotFoundException(file.relativePath());
        return template;
    }

    /**
     * Cleans the cache for all templates
     */
    public static void cleanCompiledCache() {
        templates.clear();
    }

    /**
     * Cleans the specified key from the cache
     * 
     * @param key
     *            The template key
     */
    public static void cleanCompiledCache(String key) {
        templates.remove(key);
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
