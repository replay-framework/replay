package play.db.jpa;

import play.db.jpa.GenericModel.JPAQuery;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import javax.persistence.Entity;
import javax.persistence.PersistenceUnit;
import java.util.List;

@Singleton
public class JPARepository<T extends JPABase> {
  private final String entityName;
  private String dbName;

  @Nonnull
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

  @Nonnull
  public List<T> findAll() {
    return JPQL.instance.findAll(dbName, entityName);
  }

  @Nullable
  public T findById(Object id) {
    try {
      return (T) JPQL.instance.findById(dbName, entityName, id);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nonnull
  public JPAQuery find(String query, Object... params) {
    return JPQL.instance.find(dbName, entityName, query, params);
  }

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
