package play.db.jpa;

import play.db.jpa.GenericModel.JPAQuery;
import play.mvc.Http;
import play.mvc.Scope;

import javax.inject.Singleton;
import javax.persistence.Entity;
import javax.persistence.PersistenceUnit;
import java.util.List;

@Singleton
public class JPARepository<T extends JPABase> {
  private final String entityName;
  private String dbName;

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

  public T create(Http.Request request, Scope.Session session, String name, Scope.Params params) {
    try {
      return (T) JPQL.instance.create(request, session, dbName, entityName, name, params);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
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

  public T findById(Object id) {
    try {
      return (T) JPQL.instance.findById(dbName, entityName, id);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public JPAQuery find(String query, Object... params) {
    return JPQL.instance.find(dbName, entityName, query, params);
  }

  public JPAQuery all() {
    return JPQL.instance.all(dbName, entityName);
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
