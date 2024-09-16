package play.data.binding.types;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import play.data.binding.AnnotationHelper;
import play.data.binding.TypeBinder;
import play.i18n.Lang;
import play.libs.I18N;
import play.mvc.Http;
import play.mvc.Scope;

/** Binder that support Calendar class. */
public class CalendarBinder implements TypeBinder<Calendar> {

  @Override
  public Calendar bind(
      Http.Request request,
      Scope.Session session,
      String name,
      Annotation[] annotations,
      String value,
      Class actualClass,
      Type genericType)
      throws ParseException {
    if (isBlank(value)) {
      return null;
    }
    Calendar cal = Calendar.getInstance(Lang.getLocale());

    Date date = AnnotationHelper.getDateAs(annotations, value);
    if (date != null) {
      cal.setTime(date);
    } else {
      SimpleDateFormat sdf = new SimpleDateFormat(I18N.getDateFormat());
      sdf.setLenient(false);
      cal.setTime(sdf.parse(value));
    }
    return cal;
  }
}
