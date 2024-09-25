package model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import javax.annotation.Nullable;
import play.db.jpa.GenericModel;

@MappedSuperclass
public class IdModel extends GenericModel {
  @Id @GeneratedValue protected Long id;

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
