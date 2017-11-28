package play.data.binding.types;

import org.joda.time.DateTime;
import play.data.binding.TypeBinder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Binder that support Date class.
 */
public class DateTimeBinder implements TypeBinder<DateTime> {

    private static DateBinder dateBinder = new DateBinder();

    @Override
    public DateTime bind(String name, Annotation[] annotations, String value, Class actualClass, Type genericType) throws ParseException {
        if (isBlank(value)) {
            return null;
        }
        return new DateTime(dateBinder.bind(name, annotations, value, actualClass, genericType));
    }
}
