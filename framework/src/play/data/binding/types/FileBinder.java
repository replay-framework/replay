package play.data.binding.types;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import play.data.Upload;
import play.data.binding.TypeBinder;
import play.mvc.Http;
import play.mvc.Scope;

/** Bind file form multipart/form-data request. */
public class FileBinder implements TypeBinder<File> {

  @Override
  @SuppressWarnings("unchecked")
  public File bind(
      Http.Request request,
      Scope.Session session,
      String name,
      Annotation[] annotations,
      String value,
      Class actualClass,
      Type genericType) {
    if (value == null || value.trim().isEmpty()) {
      return null;
    }
    List<Upload> uploads = (List<Upload>) request.args.get("__UPLOADS");
    if (uploads != null) {
      for (Upload upload : uploads) {
        if (upload.getFieldName().equals(value)) {
          if (!upload.getFileName().trim().isEmpty()) {
            return upload.asFile();
          }
        }
      }
    }
    return null;
  }
}
