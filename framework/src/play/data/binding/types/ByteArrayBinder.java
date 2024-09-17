package play.data.binding.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import play.data.Upload;
import play.data.binding.TypeBinder;
import play.mvc.Http;
import play.mvc.Scope;

/** Bind byte[] form multipart/form-data request. */
public class ByteArrayBinder implements TypeBinder<byte[]> {

  @SuppressWarnings("unchecked")
  @Override
  public byte[] bind(
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
          return upload.asBytes();
        }
      }
    }
    return null;
  }
}
