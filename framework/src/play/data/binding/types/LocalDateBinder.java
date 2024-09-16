package play.data.binding.types;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import play.data.binding.TypeBinder;
import play.libs.I18N;
import play.mvc.Http;
import play.mvc.Scope;

public class LocalDateBinder implements TypeBinder<LocalDate> {
  DateTimeFormatter localFormat = DateTimeFormatter.ofPattern(I18N.getDateFormat());

  @Override
  public LocalDate bind(
      Http.Request request,
      Scope.Session session,
      String name,
      Annotation[] annotations,
      String value,
      Class actualClass,
      Type genericType) {
    return isBlank(value)
        ? null
        : value.contains("-") ? LocalDate.parse(value) : LocalDate.parse(value, localFormat);
  }
}
