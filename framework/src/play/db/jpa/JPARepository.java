package play.db.jpa;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import jakarta.inject.Singleton;
import jakarta.persistence.Entity;
import jakarta.persistence.PersistenceUnit;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import play.db.jpa.GenericModel.JPAQuery;

@Singleton
@NullMarked
@CheckReturnValue
public class JPARepository<T extends JPABase> {
  private final String entityName;
  private final String dbName;

  public static <T extends JPABase> JPARepository<T> from(Class<T> modelClass) {
    return new JPARepository<>(modelClass);
  }

  private JPARepository(Class<T> modelClass) {
    if (!modelClass.isAnnotationPresent(Entity.class)) {
      throw new IllegalArgumentException("Only JPA entities are supported");
    }

    PersistenceUnit persistenceUnit = modelClass.getAnnotation(PersistenceUnit.class);
    dbName = persistenceUnit == null ? JPA.DEFAULT : persistenceUnit.name();
    entityName = modelClass.getName();
  }

  public long count() {
    return JPQL.instance.count(dbName, entityName);
  }

  public long count(String query, Object... params) {
    return JPQL.instance.count(dbName, entityName, query, params);
  }

  public List<T> findAll() {
    return JPQL.instance.findAll(dbName, entityName);
  }

  @Nullable
  @SuppressWarnings("unchecked")
  public T findById(Object id) {
      return (T) JPQL.instance.findById(dbName, entityName, id);
  }

  public JPAQuery find(String query, Object... params) {
    return JPQL.instance.find(dbName, entityName, query, params);
  }

  @CanIgnoreReturnValue
  public int delete(String query, Object... params) {
    return JPQL.instance.delete(dbName, entityName, query, params);
  }

  public int deleteAll() {
    return JPQL.instance.deleteAll(dbName, entityName);
  }

  public void save(T model) {
    model._save();
  }

  public void delete(T model) {
    model._delete();
  }
}
