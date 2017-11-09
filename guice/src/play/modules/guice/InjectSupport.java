package play.modules.guice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Deprecated
/**
 * @deprecated Don't use this feature. Static fields are evil.
 */
public @interface InjectSupport {
}
