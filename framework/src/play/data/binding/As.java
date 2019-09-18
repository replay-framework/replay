package play.data.binding;

import play.mvc.Http;
import play.mvc.Scope;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;
import java.util.Map;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
public @interface As {

    String[] value() default {""};
    String[] lang() default {"*"};
    Class<? extends TypeBinder<?>> binder() default DEFAULT.class;
    Class<? extends TypeUnbinder<?>> unbinder() default DEFAULT.class;

    final class DEFAULT implements TypeBinder<Object>, TypeUnbinder<Object> {
        @Override
        public Object bind(Http.Request request, Scope.Session session, String name, Annotation[] annotations, String value, Class actualClass, Type genericType) {
            throw new UnsupportedOperationException("Not supported.");
        }

        @Override
        public boolean unBind(Map<String, Object> result, Object src, Class<?> srcClazz, String name,
                Annotation[] annotations) {
            throw new UnsupportedOperationException("Not supported.");   
        }
    }
    
}
