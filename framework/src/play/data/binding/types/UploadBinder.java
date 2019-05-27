package play.data.binding.types;

import play.data.Upload;
import play.data.binding.Binder;
import play.data.binding.TypeBinder;
import play.db.Model;
import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.mvc.Scope;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

public class UploadBinder implements TypeBinder<Model.BinaryField> {
    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public Object bind(Http.Request request, Scope.Session session, String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            if (request.args != null) {
                List<Upload> uploads = (List<Upload>) request.args.get("__UPLOADS");
                if (uploads != null) {
                    for (Upload upload : uploads) {
                        if (upload.getFieldName().equals(value) && !upload.getFileName().trim().isEmpty()) {
                            return upload;
                        }
                    }
                }
            }

            if (request.params != null && request.params.get(value + "_delete_") != null) {
                return null;
            }
            return Binder.MISSING;
        } catch (Exception e) {
            throw new UnexpectedException("Failed to bind upload " + name, e);
        }
    }
}
