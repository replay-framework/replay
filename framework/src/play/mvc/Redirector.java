package play.mvc;

import play.data.binding.Unbinder;

import jakarta.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class is deprecated.
 *
 * Please RETURN (not throw) `play.mvc.results.Result`
 */
@Singleton
public class Redirector {

  public static Parameter param(String name, Object value) {
    return new Parameter(name, value);
  }

  private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];

  public static Map<String, Object> toMap(List<Parameter> parameters) {
    Map<String, Object> newArgs = new HashMap<>(parameters.size());
    for (Parameter parameter : parameters) {
      Unbinder.unBind(newArgs, parameter.value, parameter.name, NO_ANNOTATIONS);
    }
    return newArgs;
  }

  public static final class Parameter {
    public final String name;
    public final Object value;

    public Parameter(String name, Object value) {
      this.name = name;
      this.value = value;
    }

    @Override public boolean equals(Object other) {
      if (this == other) return true;
      if (other == null || getClass() != other.getClass()) return false;
      Parameter parameter = (Parameter) other;
      return Objects.equals(name, parameter.name) && Objects.equals(value, parameter.value);
    }

    @Override public int hashCode() {
      return Objects.hash(name, value);
    }

    @Override public String toString() {
      return name + '=' + value;
    }
  }
}
