package play.data.validation;

import net.sf.oval.configuration.annotation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This field must be a valid email.
 * Message key: validation.email
 * $1: field name
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Constraint(checkWith = EmailCheck.class)

public @interface Email {

    String message() default EmailCheck.mes;
}

