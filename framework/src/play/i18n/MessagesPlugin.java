package play.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.PlayPlugin;
import play.libs.IO;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MessagesPlugin extends PlayPlugin {
    private static final Logger logger = LoggerFactory.getLogger(MessagesPlugin.class);

    private static Long lastLoading = 0L;

    private static final List<String> includeMessageFilenames = new ArrayList<>();

    @Override
    public void onApplicationStart() {
        includeMessageFilenames.clear();
        Messages.defaults = new Properties();
        for (VirtualFile module : Play.modules.values()) {
            VirtualFile messages = module.child("conf/messages");
            if (messages != null && messages.exists()
                    && !messages.isDirectory()) {
                Messages.defaults.putAll(read(messages));
            }
        }
        VirtualFile appDM = Play.getVirtualFile("conf/messages");
        if (appDM != null && appDM.exists() && !appDM.isDirectory()) {
            Messages.defaults.putAll(read(appDM));
        }
        for (String locale : Play.langs) {
            Properties properties = new Properties();
            for (VirtualFile module : Play.modules.values()) {
                VirtualFile messages = module.child("conf/messages." + locale);
                if (messages != null && messages.exists()
                        && !messages.isDirectory()) {
                    properties.putAll(read(messages));
                }
            }
            VirtualFile appM = Play.getVirtualFile("conf/messages." + locale);
            if (appM != null && appM.exists() && !appM.isDirectory()) {
                properties.putAll(read(appM));
            } else {
                logger.warn("Messages file missing for locale {}", locale);
            }
            Messages.locales.put(locale, properties);
        }
        lastLoading = System.currentTimeMillis();
    }

    Properties read(VirtualFile vf) {
        if (vf != null) {
            return read(vf.getRealFile());
        }
        return null;
    }

    Properties read(File file) {
        Properties propsFromFile = null;
        if (file != null && !file.isDirectory()) {
            propsFromFile = IO.readUtf8Properties(file);

            // Include
            Map<Object, Object> toInclude = new HashMap<>(16);
            for (Object key : propsFromFile.keySet()) {
                if (key.toString().startsWith("@include.")) {
                    try {
                        String filenameToInclude = propsFromFile.getProperty(key.toString());
                        File fileToInclude = getIncludeFile(file, filenameToInclude);
                        if (fileToInclude != null && fileToInclude.exists() && !fileToInclude.isDirectory()) {
                            // Check if the file was not previously read
                            if (!includeMessageFilenames.contains(fileToInclude.getAbsolutePath())) {
                                toInclude.putAll(read(fileToInclude));
                                includeMessageFilenames.add(fileToInclude.getAbsolutePath());
                            }
                        } else {
                            logger.warn("Missing include: {} from file {}", filenameToInclude, file.getPath());
                        }
                    } catch (Exception ex) {
                        logger.warn("Missing include: {}, caused by: {}", key, ex);
                    }
                }
            }
            propsFromFile.putAll(toInclude);
        }
        return propsFromFile;
    }

    private File getIncludeFile(File file, String filenameToInclude) {
        if (file != null && filenameToInclude != null && !filenameToInclude.isEmpty()) {
            // Test absolute path
            File fileToInclude = new File(filenameToInclude);
            if (fileToInclude.isAbsolute()) {
                return fileToInclude;
            } else {
                return new File(file.getParent(), filenameToInclude); 
            }
        }
        return null;
    }

    @Override
    public void detectChange() {
        VirtualFile vf = Play.getVirtualFile("conf/messages");
        if (vf != null && vf.exists() && !vf.isDirectory()
                && vf.lastModified() > lastLoading) {
            onApplicationStart();
            return;
        }
        for (VirtualFile module : Play.modules.values()) {
            vf = module.child("conf/messages");
            if (vf != null && vf.exists() && !vf.isDirectory()
                    && vf.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
        for (String locale : Play.langs) {
            vf = Play.getVirtualFile("conf/messages." + locale);
            if (vf != null && vf.exists() && !vf.isDirectory()
                    && vf.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
            for (VirtualFile module : Play.modules.values()) {
                vf = module.child("conf/messages." + locale);
                if (vf != null && vf.exists() && !vf.isDirectory()
                        && vf.lastModified() > lastLoading) {
                    onApplicationStart();
                    return;
                }
            }
        }

        for (String includeFilename : includeMessageFilenames) {
            File fileToInclude = new File(includeFilename);
            if (fileToInclude != null && fileToInclude.exists()
                    && !fileToInclude.isDirectory()
                    && fileToInclude.lastModified() > lastLoading) {
                onApplicationStart();
                return;
            }
        }
    }
}