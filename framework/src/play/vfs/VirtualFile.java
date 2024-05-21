package play.vfs;

import play.exceptions.UnexpectedException;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;

import static org.apache.commons.io.FileUtils.readFileToByteArray;

/**
 * @deprecated This class is not needed anymore. 
 * It was used in Play! framework to load resources from local folder "modules" which was a custom dependency management mechanism.
 * RePlay uses standard Maven/Gradle dependencies. Folder "modules" doesn't exist anymore.
 * 
 * So we use plain old {@link java.io.File} instead.
 */
@Deprecated
public class VirtualFile {

    private final File realFile;

    private VirtualFile(File file) {
        this.realFile = file;
    }

    public String getName() {
        return realFile.getName();
    }

    public boolean exists() {
        return realFile != null && realFile.exists();
    }

    public long length() {
        return realFile.length();
    }

    @Nonnull
    @CheckReturnValue
    public VirtualFile child(String name) {
        return new VirtualFile(new File(realFile, name));
    }

    public static VirtualFile open(String file) {
        return open(new File(file));
    }

    public static VirtualFile open(File file) {
        return new VirtualFile(file);
    }

    public File getRealFile() {
        return realFile;
    }

    public URI getURI() {
        return getRealFile().toURI();
    }

    public byte[] content() {
        try {
            return readFileToByteArray(realFile);
        } catch (IOException e) {
            throw new UnexpectedException("Failed to read " + realFile.getAbsolutePath(), e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    @Deprecated
    @Nullable
    public static VirtualFile search(Collection<VirtualFile> roots, String path) {
        for (VirtualFile file : roots) {
            if (file.child(path).exists()) {
                return file.child(path);
            }
        }
        return null;
    }
}