package play.data.validation;

import static java.lang.Integer.parseInt;

import java.util.regex.Pattern;
import net.sf.oval.Validator;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import net.sf.oval.context.OValContext;
import net.sf.oval.exception.OValException;
import org.apache.commons.lang3.StringUtils;

public class IPv4AddressCheck extends AbstractAnnotationCheck<IPv4Address> {

  static final String mes = "validation.ipv4";
  private static final Pattern DOT = Pattern.compile("[.]");
  private static final Pattern ONE_TO_THREE_DIGITS = Pattern.compile("[0-9]{1,3}");

  @Override
  public void configure(IPv4Address ipv4Address) {
    setMessage(ipv4Address.message());
  }

  @Override
  public boolean isSatisfied(
      Object validatedObject, Object value, OValContext context, Validator validator)
      throws OValException {
    if (value == null || value.toString().isEmpty()) {
      return true;
    }
    try {
      String[] parts = DOT.split(value.toString());
      // Check that there is no trailing separator
      if (parts.length != 4 || StringUtils.countMatches(value.toString(), ".") != 3) {
        return false;
      }

      for (String part : parts) {
        // Check that we don't have empty part or (+-) sign
        if (part.isEmpty() || !ONE_TO_THREE_DIGITS.matcher(part).matches()) {
          return false;
        }
        int p = parseInt(part);
        if (p < 0 || p > 255) {
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
