package play.vfs;

import play.Play;
import play.exceptions.UnexpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.FileUtils.readFileToByteArray;
import static org.apache.commons.io.FileUtils.readFileToString;

public class VirtualFile {

    private final File realFile;

    private VirtualFile(File file) {
        this.realFile = file;
    }

    public String getName() {
        return realFile.getName();
    }

    public boolean isDirectory() {
        return realFile.isDirectory();
    }

    public String relativePath() {
        List<String> path = new ArrayList<>();
        File f = realFile;
        String prefix = "{?}";
        while (true) {
            path.add(f.getName());
            f = f.getParentFile();
            if (f == null) {
                break; // ??
            }
            if (f.equals(Play.applicationPath)) {
                prefix = "";
                break;
            }
            String module = isRoot(f);
            if (module != null) {
                prefix = module;
                break;
            }

        }
        Collections.reverse(path);
        StringBuilder builder = new StringBuilder(prefix);
        for (String p : path) {
            builder.append('/').append(p);
        }
        return builder.toString();
    }

    private String isRoot(File f) {
        for (VirtualFile vf : Play.roots) {
            if (vf.realFile.getAbsolutePath().equals(f.getAbsolutePath())) {
                String modulePathName = vf.getName();
                String moduleName = modulePathName.contains("-") ? modulePathName.substring(0, modulePathName.lastIndexOf("-"))
                        : modulePathName;
                return "{module:" + moduleName + "}";
            }
        }
        return null;
    }

    public List<VirtualFile> list() {
        List<VirtualFile> res = new ArrayList<>();
        if (exists()) {
            File[] children = realFile.listFiles();
            if (children != null) {
                for (File aChildren : children) {
                    res.add(new VirtualFile(aChildren));
                }
            }
        }
        return res;
    }

    public boolean exists() {
        return realFile != null && realFile.exists();
    }

    public InputStream inputstream() {
        try {
            return new FileInputStream(realFile);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public Long lastModified() {
        if (realFile != null) {
            return realFile.lastModified();
        }
        return 0L;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof VirtualFile) {
            VirtualFile vf = (VirtualFile) other;
            if (realFile != null && vf.realFile != null) {
                return realFile.equals(vf.realFile);
            }
        }
        return super.equals(other);
    }

    @Override
    public int hashCode() {
        if (realFile != null) {
            return realFile.hashCode();
        }
        return super.hashCode();
    }

    public long length() {
        return realFile.length();
    }

    public VirtualFile child(String name) {
        return new VirtualFile(new File(realFile, name));
    }

    public static VirtualFile open(String file) {
        return open(new File(file));
    }

    public static VirtualFile open(File file) {
        return new VirtualFile(file);
    }

    public String contentAsString() {
        try {
            return readFileToString(realFile, UTF_8);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    public File getRealFile() {
        return realFile;
    }

    public byte[] content() {
        try {
            return readFileToByteArray(realFile);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public String toString() {
        return getName();
    }

    public static VirtualFile search(Collection<VirtualFile> roots, String path) {
        for (VirtualFile file : roots) {
            if (file.child(path).exists()) {
                return file.child(path);
            }
        }
        return null;
    }

    public static VirtualFile fromRelativePath(String relativePath) {
        if (relativePath == null) {
            return null;
        }
        Pattern pattern = Pattern.compile("^(\\{(.+?)})?(.*)$");
        Matcher matcher = pattern.matcher(relativePath);

        if (matcher.matches()) {
            String path = matcher.group(3);
            String module = matcher.group(2);
            if (module == null || module.equals("?") || module.equals("")) {
                return new VirtualFile(Play.applicationPath).child(path);
            }
            else if (module.startsWith("module:")) {
                module = module.substring("module:".length());
                for (Entry<String, VirtualFile> entry : Play.modules.entrySet()) {
                    if (entry.getKey().equals(module))
                        return entry.getValue().child(path);
                }
            }
        }

        return null;
    }
}