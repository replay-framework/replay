package play.data.validation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import play.exceptions.UnexpectedException;
import play.libs.I18N;
import play.utils.Utils.AlternativeDateFormat;

public class InFutureCheck extends AbstractAnnotationCheck<InFuture> {

  static final String mes = "validation.future";

  Date reference;

  @Override
  public void configure(InFuture future) {
    try {
      this.reference = future.value().isEmpty()
          ? new Date()
          : AlternativeDateFormat.getDefaultFormatter().parse(future.value());
    } catch (ParseException ex) {
      throw new UnexpectedException("Cannot parse date " + future.value(), ex);
    }
    if (!future.value().isEmpty() && future.message().equals(mes)) {
      setMessage("validation.after");
    } else {
      setMessage(future.message());
    }
  }

  @Override
  public boolean isSatisfied(
      Object validatedObject, Object value, OValContext context, Validator validator) {
    requireMessageVariablesRecreation();
    if (value == null) {
      return true;
    }
    if (value instanceof Date) {
      try {
        return reference.before((Date) value);
      } catch (Exception e) {
        return false;
      }
    }
    if (value instanceof Long) {
      try {
        return reference.before(new Date((Long) value));
      } catch (Exception e) {
        return false;
      }
    }
    return false;
  }

  @Override
  public Map<String, String> createMessageVariables() {
    Map<String, String> messageVariables = new HashMap<>();
    messageVariables.put("reference", new SimpleDateFormat(I18N.getDateFormat()).format(reference));
    return messageVariables;
  }
}
