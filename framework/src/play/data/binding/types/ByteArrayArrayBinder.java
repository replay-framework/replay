package play.data.binding.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import play.data.Upload;
import play.data.binding.TypeBinder;
import play.mvc.Http;
import play.mvc.Scope;

/**
 * Bind byte[][] form multipart/form-data request, so you can have a byte[][] in your controller.
 * This is useful when you have a multiple on your input file.
 */
public class ByteArrayArrayBinder implements TypeBinder<byte[][]> {

  @SuppressWarnings("unchecked")
  @Override
  public byte[][] bind(
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
      List<byte[]> byteList = new ArrayList<>();
      List<Upload> uploads = (List<Upload>) request.args.get("__UPLOADS");
      if (uploads != null) {
        for (Upload upload : uploads) {
          if (upload.getFieldName().equals(value)) {
            byteList.add(upload.asBytes());
          }
        }
      }
      return byteList.toArray(new byte[byteList.size()][]);
    }
    return null;
  }
}
