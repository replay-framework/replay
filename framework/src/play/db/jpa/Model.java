package play.db.jpa;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import javax.annotation.Nullable;

/**
 * Base class for JPA model objects.
 * Automatically provides an @Id Long id field.
 */
@MappedSuperclass
public class Model extends GenericModel {

    @Id
    @GeneratedValue
    protected Long id;

    public @Nullable Long getId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    @Override
    public Object _key() {
        return getId();
    }

}
