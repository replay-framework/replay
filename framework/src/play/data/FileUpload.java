package play.data;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import play.data.parsing.TempFilePlugin;
import play.exceptions.UnexpectedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUpload implements Upload {

    private final FileItem fileItem;
    private final File defaultFile;

    public FileUpload(FileItem fileItem) {
        this.fileItem = fileItem;
        File tmp = TempFilePlugin.createTempFolder();
        // Check that the file has a name to avoid to override the field folder
        if (fileItem.getName().trim().isEmpty()) {
            defaultFile = null;
        }
        else {
            defaultFile = new File(tmp, FilenameUtils.getName(fileItem.getFieldName()) + File.separator
                    + FilenameUtils.getName(fileItem.getName()));
            try {
                if (!defaultFile.getCanonicalPath().startsWith(tmp.getCanonicalPath())) {
                    throw new IllegalStateException("Temp file " + tmp.getCanonicalPath() + " try to override existing file " + defaultFile.getCanonicalPath());
                }
                defaultFile.getParentFile().mkdirs();
                fileItem.write(defaultFile);
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Error when trying to write to file " + defaultFile.getAbsolutePath(), e);
            }
        }
    }

    @Override
    public File asFile() {
        return defaultFile;
    }

    @Override
    public byte[] asBytes() {
        try {
            return FileUtils.readFileToByteArray(defaultFile);
        } catch (IOException e) {
            throw new UnexpectedException(e);
        }
    }

    @Override
    public InputStream asStream() {
        try {
            return new FileInputStream(defaultFile);
        } catch (IOException ex) {
            throw new UnexpectedException(ex);
        }
    }

    @Override
    public String getContentType() {
        return fileItem.getContentType();
    }

    @Override
    public String getFileName() {
        return fileItem.getName();
    }

    @Override
    public String getFieldName() {
        return fileItem.getFieldName();
    }

    @Override
    public long getSize() {
        return defaultFile == null ? 0 : defaultFile.length();
    }
}
