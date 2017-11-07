package play.mvc;

import play.data.binding.Unbinder;
import play.mvc.results.Redirect;
import play.rebel.RedirectToAction;

import javax.annotation.Nullable;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Arrays.asList;

@Singleton
public class Redirector {

  public void to(String action, Parameter... parameters) {
    to(action, asList(parameters));
  }
  
  public void to(String action, List<Parameter> parameters) {
    to(action, toMap(parameters));
  }
  
  public void to(String action, Map<String, Object> parameters) {
    if ((action.startsWith("/") || action.startsWith("http://") || action.startsWith("https://")) && parameters.isEmpty()) {
      toUrl(action);
    }
    
    throw new RedirectToAction(action, parameters);
  }

  public void toUrl(String url) {
    throw new Redirect(url, false);
  }

  public void toUrl(Url url) {
    toUrl(url.toString());
  }

  public void toUrl(String url, Map<String, Object> parameters) {
    toUrl(new Url(url, parameters));
  }

  public void toUrl(String url, String paramName, @Nullable Object paramValue) {
    toUrl(new Url(url, paramName, paramValue));
  }

  public void toUrl(String url,
                    String param1Name, @Nullable Object param1value,
                    String param2name, @Nullable Object param2value) {
    toUrl(new Url(url, param1Name, param1value, param2name, param2value));
  }

  public void toUrl(String url,
                    String param1Name, @Nullable Object param1value,
                    String param2name, @Nullable Object param2value,
                    String param3name, @Nullable Object param3value) {
    toUrl(new Url(url,
        param1Name, param1value,
        param2name, param2value,
        param3name, param3value));
  }

  public void toUrl(String url,
                    String param1Name, @Nullable Object param1value,
                    String param2name, @Nullable Object param2value,
                    String param3name, @Nullable Object param3value,
                    String param4name, @Nullable Object param4value) {
    toUrl(new Url(url,
        param1Name, param1value,
        param2name, param2value,
        param3name, param3value,
        param4name, param4value));
  }

  public void toUrl(String url,
                    String param1Name, @Nullable Object param1value,
                    String param2name, @Nullable Object param2value,
                    String param3name, @Nullable Object param3value,
                    String param4name, @Nullable Object param4value,
                    String param5name, @Nullable Object param5value) {
    toUrl(new Url(url,
        param1Name, param1value,
        param2name, param2value,
        param3name, param3value,
        param4name, param4value,
        param5name, param5value));
  }

  public Builder with(String name, Object value) {
    return new Builder(name, value);
  }

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

  public class Builder {
    private final Map<String, Object> parameters = new HashMap<>();

    public Builder(String name, Object value) {
      with(name, value);
    }

    public final Builder with(String name, Object value) {
      Unbinder.unBind(parameters, value, name, NO_ANNOTATIONS);
      return this;
    }

    public void to(String action) {
      Redirector.this.to(action, parameters);
    }
  }
}
