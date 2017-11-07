package play.data.binding.types;

import play.data.Upload;
import play.data.binding.TypeBinder;
import play.mvc.Http.Request;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Bind file form multipart/form-data request.
 */
public class FileBinder implements TypeBinder<File> {

    @Override
    @SuppressWarnings("unchecked")
    public File bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        Request req = Request.current();
        if (req != null && req.args != null) {
            List<Upload> uploads = (List<Upload>) req.args.get("__UPLOADS");
            if (uploads != null) {
                for (Upload upload : uploads) {
                    if (upload.getFieldName().equals(value)) {
                        if (upload.getFileName().trim().length() > 0) {
                            File file = upload.asFile();
                            return file;
                        }
                    }
                }
            }
        }
        return null;
    }
}
