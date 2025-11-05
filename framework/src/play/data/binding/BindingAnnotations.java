package play.data.binding;

import com.google.errorprone.annotations.CheckReturnValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.lang.annotation.Annotation;

@NullMarked
@CheckReturnValue
public class BindingAnnotations {

  public final Annotation @Nullable [] annotations;
  private String @Nullable [] profiles;
  private String @Nullable [] noBindingProfiles;

  public BindingAnnotations(Annotation @Nullable [] annotations) {
    this.annotations = annotations;
  }

  public BindingAnnotations(Annotation[] annotations, String[] profiles) {
    this(annotations);
    this.profiles = profiles;
  }

  public String[] getProfiles() {
    if (profiles != null) {
      return profiles;
    }

    if (annotations != null) {
      for (Annotation annotation : annotations) {
        if (annotation.annotationType().equals(As.class)) {
          As as = ((As) annotation);
          profiles = as.value();
        }
        if (annotation.annotationType().equals(NoBinding.class)) {
          NoBinding bind = ((NoBinding) annotation);
          profiles = bind.value();
        }
      }
    }
    if (profiles == null) {
      profiles = new String[0];
    }

    return profiles;
  }

  private String[] getNoBindingProfiles() {
    if (noBindingProfiles != null) {
      return noBindingProfiles;
    }

    if (annotations != null) {
      for (Annotation annotation : annotations) {
        if (annotation.annotationType().equals(NoBinding.class)) {
          NoBinding bind = ((NoBinding) annotation);
          noBindingProfiles = bind.value();
        }
      }
    }

    if (noBindingProfiles == null) {
      noBindingProfiles = new String[0];
    }
    return noBindingProfiles;
  }

  public boolean checkNoBinding() {

    String[] localProfiles = getProfiles();
    String[] localNoBindingProfiles = getNoBindingProfiles();

    for (String l : localNoBindingProfiles) {
      if ("*".equals(l)) {
        return true;
      }
      for (String p : localProfiles) {
        if (l.equals(p) || "*".equals(p)) {
          return true;
        }
      }
    }
    return false;
  }
}
