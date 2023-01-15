package play.data.binding.types;

import play.data.binding.TypeBinder;
import play.mvc.Http;
import play.mvc.Scope;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDateTime;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class LocalDateTimeBinder implements TypeBinder<LocalDateTime> {
  @Override
  public LocalDateTime bind(Http.Request request, Scope.Session session, String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
    return isBlank(value) ? null : LocalDateTime.parse(value);
  }
}