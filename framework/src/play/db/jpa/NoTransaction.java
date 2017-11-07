package play.db.jpa;

import java.lang.annotation.*;

/**
 * Annotation to be used on methods telling JPA that it should not create a Transaction at all
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(value = { ElementType.METHOD, ElementType.TYPE })
public @interface NoTransaction {
    /**
     * Db name we do not want transaction.
     * 
     * @return The DB name
     */
    String value() default "default";
}
