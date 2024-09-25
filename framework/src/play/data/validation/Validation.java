package play.data.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.sf.oval.configuration.annotation.AbstractAnnotationCheck;
import play.exceptions.UnexpectedException;

public class Validation {

  public static final ThreadLocal<Validation> current = new ThreadLocal<>();
  List<Error> errors = new ArrayList<>();
  boolean keep;

  protected Validation() {}

  /** @return The current validation helper */
  public static Validation current() {
    return current.get();
  }

  /** @return The list of all errors */
  @SuppressWarnings({"unused"})
  public static List<Error> errors() {
    Validation validation = current.get();
    if (validation == null) return emptyList();

    return new ArrayList<Error>(validation.errors) {

      public Error forKey(String key) {
        return Validation.error(key);
      }

      public List<Error> allForKey(String key) {
        return Validation.errors(key);
      }
    };
  }

  /** @return All errors keyed by field name */
  public Map<String, List<Error>> errorsMap() {
    Map<String, List<Error>> result = new LinkedHashMap<>();
    for (Error error : errors()) {
      result.put(error.getKey(), errors(error.getKey()));
    }
    return result;
  }

  /**
   * Add an error
   *
   * @param field Field name
   * @param message Message key
   * @param variables Message variables
   */
  public static void addError(String field, String message, String... variables) {
    insertError(Validation.current().errors.size(), field, message, variables);
  }

  /**
   * Insert an error at the specified position in this list.
   *
   * @param index index at which the specified element is to be inserted
   * @param field Field name
   * @param message Message key
   * @param variables Message variables
   */
  public static void insertError(int index, String field, String message, String... variables) {
    Error error = error(field);
    if (error == null || !error.getMessageKey().equals(message)) {
      Validation.current().errors.add(index, new Error(field, message, asList(variables)));
    }
  }

  /**
   * Remove all errors on a field with the given message
   *
   * @param field Field name
   * @param message Message key
   */
  public static void removeErrors(String field, String message) {
    Validation validation = current.get();
    if (validation != null) {
      Iterator<Error> it = validation.errors.iterator();
      while (it.hasNext()) {
        Error error = it.next();
        if (error.getKey() != null
            && error.getKey().equals(field)
            && error.getMessageKey().equals(message)) {
          it.remove();
        }
      }
    }
  }

  /**
   * Remove all errors on a field
   *
   * @param field Field name
   */
  public static void removeErrors(String field) {
    Validation validation = current.get();
    if (validation != null) {
      validation.errors.removeIf(error -> error.getKey() != null && error.getKey().equals(field));
    }
  }

  /** @return True if the current request has errors */
  public static boolean hasErrors() {
    Validation validation = current.get();
    return validation != null && !validation.errors.isEmpty();
  }

  /**
   * @param field The field name
   * @return true if field has some errors
   */
  public static boolean hasErrors(String field) {
    return error(field) != null;
  }

  /**
   * @param field The field name
   * @return First error related to this field
   */
  public static Error error(String field) {
    Validation validation = current.get();
    if (validation == null) return null;

    for (Error error : validation.errors) {
      if (error.getKey() != null && error.getKey().equals(field)) {
        return error;
      }
    }
    return null;
  }

  /**
   * @param field The field name
   * @return All errors related to this field
   */
  public static List<Error> errors(String field) {
    Validation validation = current.get();
    if (validation == null) return emptyList();

    List<Error> errors = new ArrayList<>();
    for (Error error : validation.errors) {
      if (error.getKey() != null && error.getKey().equals(field)) {
        errors.add(error);
      }
    }
    return errors;
  }

  /** Keep errors for the next request (will be stored in a cookie) */
  public static void keep() {
    current.get().keep = true;
  }

  /**
   * @param field The field name
   * @return True is there are errors related to this field
   */
  public static boolean hasError(String field) {
    return error(field) != null;
  }

  public static void clear() {
    current.get().errors.clear();
    if (ValidationPlugin.keys.get() != null) {
      ValidationPlugin.keys.get().clear();
    }
  }

  // ~~~~ Integration helper
  public static Map<String, List<Validator>> getValidators(Class<?> clazz, String name) {
    Map<String, List<Validator>> result = new HashMap<>();
    searchValidator(clazz, name, result);
    return result;
  }

  public static List<Validator> getValidators(Class<?> clazz, String property, String name) {
    try {
      List<Validator> validators = new ArrayList<>();
      while (!clazz.equals(Object.class)) {
        try {
          Field field = clazz.getDeclaredField(property);
          for (Annotation annotation : field.getDeclaredAnnotations()) {
            if (annotation.annotationType().getName().startsWith("play.data.validation")) {
              Validator validator = new Validator(annotation);
              validators.add(validator);
              if (annotation.annotationType().equals(InFuture.class)) {
                validator.params.put("reference", ((InFuture) annotation).value());
              }
              if (annotation.annotationType().equals(InPast.class)) {
                validator.params.put("reference", ((InPast) annotation).value());
              }
            }
          }
          break;
        } catch (NoSuchFieldException e) {
          clazz = clazz.getSuperclass();
        }
      }
      return validators;
    } catch (Exception e) {
      return new ArrayList<>();
    }
  }

  static void searchValidator(Class<?> clazz, String name, Map<String, List<Validator>> result) {
    for (Field field : clazz.getDeclaredFields()) {

      List<Validator> validators = new ArrayList<>();
      String key = name + "." + field.getName();
      boolean containsAtValid = false;
      for (Annotation annotation : field.getDeclaredAnnotations()) {
        if (annotation.annotationType().getName().startsWith("play.data.validation")) {
          Validator validator = new Validator(annotation);
          validators.add(validator);
          if (annotation.annotationType().equals(InFuture.class)) {
            validator.params.put("reference", ((InFuture) annotation).value());
          }
          if (annotation.annotationType().equals(InPast.class)) {
            validator.params.put("reference", ((InPast) annotation).value());
          }
        }
        if (annotation.annotationType().equals(Valid.class)) {
          containsAtValid = true;
        }
      }
      if (!validators.isEmpty()) {
        result.put(key, validators);
      }
      if (containsAtValid) {
        searchValidator(field.getType(), key, result);
      }
    }
  }

  public static class Validator {

    public Annotation annotation;
    public Map<String, Object> params = new HashMap<>();

    public Validator(Annotation annotation) {
      this.annotation = annotation;
    }
  }

  public static class ValidationResult {
    public boolean ok;
    public Error error;

    public ValidationResult message(String message) {
      if (error != null) {
        error = new Error(message, error.getKey(), error.getVariables());
      }
      return this;
    }

    public ValidationResult key(String key) {
      if (error != null) {
        error = new Error(error.getMessageKey(), key, error.getVariables());
      }
      return this;
    }
  }

  public static ValidationResult required(String key, Object o) {
    RequiredCheck check = new RequiredCheck();
    return applyCheck(check, key, o);
  }

  public static ValidationResult min(String key, Object o, double min) {
    MinCheck check = new MinCheck();
    check.min = min;
    return applyCheck(check, key, o);
  }

  public static ValidationResult max(String key, Object o, double max) {
    MaxCheck check = new MaxCheck();
    check.max = max;
    return applyCheck(check, key, o);
  }

  public static ValidationResult future(String key, Object o, Date reference) {
    InFutureCheck check = new InFutureCheck();
    check.reference = reference;
    return applyCheck(check, key, o);
  }

  public static ValidationResult future(String key, Object o) {
    InFutureCheck check = new InFutureCheck();
    check.reference = new Date();
    return applyCheck(check, key, o);
  }

  public static ValidationResult past(String key, Object o, Date reference) {
    InPastCheck check = new InPastCheck();
    check.reference = reference;
    return applyCheck(check, key, o);
  }

  public static ValidationResult past(String key, Object o) {
    InPastCheck check = new InPastCheck();
    check.reference = new Date();
    return applyCheck(check, key, o);
  }

  public static ValidationResult match(String key, Object o, String pattern) {
    MatchCheck check = new MatchCheck();
    check.pattern = Pattern.compile(pattern);
    return applyCheck(check, key, o);
  }

  public static ValidationResult email(String key, Object o) {
    EmailCheck check = new EmailCheck();
    return applyCheck(check, key, o);
  }

  public static ValidationResult url(String key, Object o) {
    URLCheck check = new URLCheck();
    return applyCheck(check, key, o);
  }

  public static ValidationResult phone(String key, Object o) {
    PhoneCheck check = new PhoneCheck();
    return applyCheck(check, key, o);
  }

  public static ValidationResult ipv4Address(String key, Object o) {
    IPv4AddressCheck check = new IPv4AddressCheck();
    return applyCheck(check, key, o);
  }

  public static ValidationResult ipv6Address(String key, Object o) {
    IPv6AddressCheck check = new IPv6AddressCheck();
    return applyCheck(check, key, o);
  }

  public static ValidationResult isTrue(String key, Object o) {
    IsTrueCheck check = new IsTrueCheck();
    return applyCheck(check, key, o);
  }

  public static ValidationResult range(String key, Object o, double min, double max) {
    RangeCheck check = new RangeCheck();
    check.min = min;
    check.max = max;
    return applyCheck(check, key, o);
  }

  public static ValidationResult minSize(String key, Object o, int minSize) {
    MinSizeCheck check = new MinSizeCheck();
    check.minSize = minSize;
    return applyCheck(check, key, o);
  }

  public static ValidationResult maxSize(String key, Object o, int maxSize) {
    MaxSizeCheck check = new MaxSizeCheck();
    check.maxSize = maxSize;
    return applyCheck(check, key, o);
  }

  public static ValidationResult valid(String key, Object o) {
    ValidCheck check = new ValidCheck();
    check.key = key;
    return applyCheck(check, key, o);
  }

  static ValidationResult applyCheck(AbstractAnnotationCheck<?> check, String key, Object o) {
    try {
      ValidationResult result = new ValidationResult();
      if (!check.isSatisfied(o, o, null, null)) {
        Error error =
            new Error(
                key,
                check.getClass().getDeclaredField("mes").get(null) + "",
                check.getMessageVariables() == null
                    ? emptyList()
                    : check.getMessageVariables().values());
        Validation.current().errors.add(error);
        result.error = error;
        result.ok = false;
      } else {
        result.ok = true;
      }
      return result;
    } catch (Exception e) {
      throw new UnexpectedException(e);
    }
  }
}
