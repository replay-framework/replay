package play.data.binding.types;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import play.data.binding.TypeBinder;
import play.mvc.Http;
import play.mvc.Scope;

public class LocalDateTimeBinder implements TypeBinder<LocalDateTime> {
  @Override
  public LocalDateTime bind(
      Http.Request request,
      Scope.Session session,
      String name,
      Annotation[] annotations,
      String value,
      Class actualClass,
      Type genericType) {
    return isBlank(value) ? null : LocalDateTime.parse(value);
  }
}
