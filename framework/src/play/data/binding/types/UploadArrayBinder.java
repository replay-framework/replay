package play.data.binding.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import play.data.Upload;
import play.data.binding.TypeBinder;
import play.db.Model;
import play.mvc.Http;
import play.mvc.Scope;

/**
 * Bind file form multipart/form-data request to an array of Upload object. This is useful when you
 * have a multiple on your input file.
 */
public class UploadArrayBinder implements TypeBinder<Model.BinaryField[]> {

  @SuppressWarnings("unchecked")
  @Override
  public Upload[] bind(
      Http.Request request,
      Scope.Session session,
      String name,
      Annotation[] annotations,
      String value,
      Class actualClass,
      Type genericType) {
    if (value == null || value.trim().length() == 0) {
      return null;
    }
    if (request.args != null) {
      List<Upload> uploadArray = new ArrayList<>();
      List<Upload> uploads = (List<Upload>) request.args.get("__UPLOADS");
      if (uploads != null) {
        for (Upload upload : uploads) {
          if (upload.getFieldName().equals(value)) {
            uploadArray.add(upload);
          }
        }
        return uploadArray.toArray(new Upload[uploadArray.size()]);
      }
    }
    return null;
  }
}
