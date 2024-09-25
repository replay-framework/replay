package play.data.binding.types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Locale;
import play.data.binding.TypeBinder;
import play.mvc.Http;
import play.mvc.Scope;

/** Binder that support Locale class. */
public class LocaleBinder implements TypeBinder<Locale> {

  @Override
  public Locale bind(
      Http.Request request,
      Scope.Session session,
      String name,
      Annotation[] annotations,
      String value,
      Class actualClass,
      Type genericType) {
    if (value == null) return null;
    if (value.length() == 2) {
      return new Locale(value);
    }
    if (value.length() == 5) {
      return new Locale(value.substring(0, 2), value.substring(3, 5));
    }
    if (value.length() == 8) {
      return new Locale(value.substring(0, 2), value.substring(3, 5), value.substring(6, 8));
    }
    return null;
  }
}
