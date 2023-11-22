package play.libs;

import play.exceptions.UnexpectedException;
import play.utils.OrderSafeProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToString;

public class IO {
    public static Properties readUtf8Properties(File file) {
        try (InputStream in = new FileInputStream(file)) {
            return readUtf8Properties(in);
        }
        catch (IOException e) {
            throw new UnexpectedException("Failed to read " + file.getAbsolutePath(), e);
        }
    }

    public static Properties readUtf8Properties(String fileName) throws IOException {
        try (InputStream in = IO.class.getResourceAsStream(fileName)) {
            return readUtf8Properties(in);
        }
    }

    public static Properties readUtf8Properties(InputStream is) throws IOException {
        Properties properties = new OrderSafeProperties();
        properties.load(is);
        return properties;
    }

    public static String contentAsString(File file) {
        try {
            return readFileToString(file, UTF_8);
        } catch (IOException e) {
            throw new UnexpectedException("Failed to read " + file.getAbsolutePath(), e);
        }
    }
}
