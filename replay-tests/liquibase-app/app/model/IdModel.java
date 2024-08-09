package model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import play.db.jpa.GenericModel;

import javax.annotation.Nullable;

@MappedSuperclass
public class IdModel extends GenericModel {
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
