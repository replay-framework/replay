package play.libs;

import org.apache.commons.io.FileUtils;
import play.exceptions.UnexpectedException;
import play.utils.OrderSafeProperties;
import play.vfs.VirtualFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;

public class IO {
    public static Properties readUtf8Properties(VirtualFile file) {
        try (InputStream in = file.inputstream()) {
            return readUtf8Properties(in);
        }
        catch (IOException e) {
            throw new UnexpectedException("Failed to read " + file.relativePath(), e);
        }
    }

    public static Properties readUtf8Properties(File file) {
        try (InputStream in = new FileInputStream(file)) {
            return readUtf8Properties(in);
        }
        catch (IOException e) {
            throw new UnexpectedException("Failed to read " + file.getAbsolutePath(), e);
        }
    }

    public static Properties readUtf8Properties(InputStream is) throws IOException {
        Properties properties = new OrderSafeProperties();
        properties.load(is);
        return properties;
    }

    public static String readContentAsString(File file, Charset encoding) {
        try {
            return FileUtils.readFileToString(file, encoding);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }
}
