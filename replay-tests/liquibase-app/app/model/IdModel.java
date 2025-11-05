package model;

import com.google.errorprone.annotations.CheckReturnValue;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import play.db.jpa.GenericModel;

@MappedSuperclass
@NullMarked
@CheckReturnValue
public class IdModel extends GenericModel {
  @Id
  @GeneratedValue
  @Nullable
  protected Long id;

  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  @Nullable
  @Override
  public Object _key() {
    return getId();
  }
}
