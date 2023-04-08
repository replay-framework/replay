package play.data.parsing;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.PlayPlugin;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.StringUtils.leftPad;

/**
 *  Creates temporary folders for file parsing, and deletes
 *  it after request completion.
 */
public class TempFilePlugin extends PlayPlugin {
    private static final Logger log = LoggerFactory.getLogger(TempFilePlugin.class);
    private static final AtomicLong count = new AtomicLong();

    public static final ThreadLocal<File> tempFolder = new ThreadLocal<>();

    public static File createTempFolder() {
        if (Play.tmpDir == null) {
            throw new IllegalStateException("Cannot create temp folder: Play.tmpDir is null");
        }
        if (tempFolder.get() == null) {
            File file = new File(new File(Play.tmpDir, "uploads"),
                    System.currentTimeMillis() + "_" + leftPad(String.valueOf(count.getAndIncrement()), 10, '0'));
            if (!file.exists() && !file.mkdirs()) {
                log.error("Failed to create directory {}", file);
            }
            tempFolder.set(file);
        }
        return tempFolder.get();
    }

    @Override
    public void onInvocationSuccess() {
        File file = tempFolder.get();
        if (file != null) {
            tempFolder.remove();
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                log.error("Failed to delete directory {}", file, e);
            }
        }
    }
}
