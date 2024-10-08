package play.db.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PersistenceException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.collection.spi.PersistentMap;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;
import play.db.Model;
import play.exceptions.UnexpectedException;

/** A super class for JPA entities */
@MappedSuperclass
public class JPABase implements Serializable, play.db.Model {

  @Override
  public void _save() {
    String dbName = JPA.getDBName(this.getClass());
    if (!em(dbName).contains(this)) {
      em(dbName).persist(this);
    }
    avoidCascadeSaveLoops.set(new HashSet<>());
    try {
      saveAndCascade(true);
    } finally {
      avoidCascadeSaveLoops.get().clear();
    }
    try {
      em(dbName).flush();
    } catch (PersistenceException e) {
      if (e.getCause() instanceof GenericJDBCException) {
        throw new PersistenceException(((GenericJDBCException) e.getCause()).getSQL(), e);
      } else {
        throw e;
      }
    }
    avoidCascadeSaveLoops.set(new HashSet<>());
    try {
      saveAndCascade(false);
    } finally {
      avoidCascadeSaveLoops.get().clear();
    }
  }

  @Override
  public void _delete() {
    String dbName = JPA.getDBName(this.getClass());

    try {
      avoidCascadeSaveLoops.set(new HashSet<>());
      try {
        saveAndCascade(true);
      } finally {
        avoidCascadeSaveLoops.get().clear();
      }
      em(dbName).remove(this);
      try {
        em(dbName).flush();
      } catch (PersistenceException e) {
        if (e.getCause() instanceof GenericJDBCException) {
          throw new PersistenceException(((GenericJDBCException) e.getCause()).getSQL(), e);
        } else {
          throw e;
        }
      }
      avoidCascadeSaveLoops.set(new HashSet<>());
      try {
        saveAndCascade(false);
      } finally {
        avoidCascadeSaveLoops.get().clear();
      }
    } catch (PersistenceException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Object _key() {
    return Model.Manager.factoryFor(this.getClass()).keyValue(this);
  }

  // ~~~ SAVING
  public transient boolean willBeSaved;
  private static final ThreadLocal<Set<JPABase>> avoidCascadeSaveLoops = new ThreadLocal<>();

  private void saveAndCascade(boolean willBeSaved) {
    this.willBeSaved = willBeSaved;
    if (avoidCascadeSaveLoops.get().contains(this)) {
      return;
    } else {
      avoidCascadeSaveLoops.get().add(this);
    }
    // Cascade save
    try {
      Set<Field> fields = new HashSet<>();
      Class clazz = this.getClass();
      while (!clazz.equals(JPABase.class)) {
        Collections.addAll(fields, clazz.getDeclaredFields());
        clazz = clazz.getSuperclass();
      }
      for (Field field : fields) {
        field.setAccessible(true);
        if (Modifier.isTransient(field.getModifiers())) {
          continue;
        }
        boolean doCascade = false;
        if (field.isAnnotationPresent(OneToOne.class)) {
          doCascade = cascadeAll(field.getAnnotation(OneToOne.class).cascade());
        }
        if (field.isAnnotationPresent(OneToMany.class)) {
          doCascade = cascadeAll(field.getAnnotation(OneToMany.class).cascade());
        }
        if (field.isAnnotationPresent(ManyToOne.class)) {
          doCascade = cascadeAll(field.getAnnotation(ManyToOne.class).cascade());
        }
        if (field.isAnnotationPresent(ManyToMany.class)) {
          doCascade = cascadeAll(field.getAnnotation(ManyToMany.class).cascade());
        }
        if (doCascade) {
          Object value = field.get(this);
          if (value != null) {
            if (value instanceof PersistentMap) {
              if (((PersistentMap<?, ?>) value).wasInitialized()) {

                cascadeOrphans((PersistentCollection) value, willBeSaved);

                for (Object o : ((Map) value).values()) {
                  saveAndCascadeIfJPABase(o, willBeSaved);
                }
              }
            } else if (value instanceof PersistentCollection) {
              PersistentCollection col = (PersistentCollection) value;
              if (((PersistentCollection) value).wasInitialized()) {

                cascadeOrphans((PersistentCollection) value, willBeSaved);

                for (Object o : (Collection) value) {
                  saveAndCascadeIfJPABase(o, willBeSaved);
                }
              } else {
                cascadeOrphans(col, willBeSaved);

                for (Object o : (Collection) value) {
                  saveAndCascadeIfJPABase(o, willBeSaved);
                }
              }
            } else if (value instanceof Collection) {
              for (Object o : (Collection) value) {
                saveAndCascadeIfJPABase(o, willBeSaved);
              }
            } else if (value instanceof HibernateProxy && value instanceof JPABase) {
              if (!((HibernateProxy) value).getHibernateLazyInitializer().isUninitialized()) {
                ((JPABase)
                        ((HibernateProxy) value).getHibernateLazyInitializer().getImplementation())
                    .saveAndCascade(willBeSaved);
              }
            } else if (value instanceof JPABase) {
              ((JPABase) value).saveAndCascade(willBeSaved);
            }
          }
        }
      }
    } catch (Exception e) {
      throw new UnexpectedException("During cascading save()", e);
    }
  }

  private void cascadeOrphans(PersistentCollection persistentCollection, boolean willBeSaved) {
    String dbName = JPA.getDBName(this.getClass());

    SessionImpl session = JPA.em(dbName).unwrap(SessionImpl.class);
    PersistenceContext pc = session.getPersistenceContext();
    CollectionEntry ce = pc.getCollectionEntry(persistentCollection);

    if (ce != null) {
      CollectionPersister cp = ce.getLoadedPersister();
      if (cp != null) {
        Type ct = cp.getElementType();
        if (ct instanceof EntityType) {
          String entityName = ((EntityType) ct).getAssociatedEntityName(session.getFactory());
          if (ce.getSnapshot() != null) {
            Collection<?> orphans = ce.getOrphans(entityName, persistentCollection);
            for (Object o : orphans) {
              saveAndCascadeIfJPABase(o, willBeSaved);
            }
          }
        }
      }
    }
  }

  private static void saveAndCascadeIfJPABase(Object o, boolean willBeSaved) {
    if (o instanceof JPABase) {
      ((JPABase) o).saveAndCascade(willBeSaved);
    }
  }

  private static boolean cascadeAll(CascadeType[] types) {
    for (CascadeType cascadeType : types) {
      if (cascadeType == CascadeType.ALL || cascadeType == CascadeType.PERSIST) {
        return true;
      }
    }
    return false;
  }

  /**
   * Retrieve the current entityManager
   *
   * @param name The DB name
   * @return the current entityManager
   */
  public static EntityManager em(String name) {
    return JPA.em(name);
  }

  public static EntityManager em() {
    return JPA.em();
  }

  public boolean isPersistent(String name) {
    return JPA.em(name).contains(this);
  }

  public boolean isPersistent() {
    return JPA.em().contains(this);
  }

  /**
   * JPASupport instances a and b are equals if either <strong>a == b</strong> or a and b have same
   * <strong>{@link #_key key} and class</strong>
   *
   * @param other The object to compare to
   * @return true if equality condition above is verified
   */
  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (this == other) {
      return true;
    }

    Object key = this._key();
    if (key == null) {
      return false;
    }
    if (play.db.Model.class.isAssignableFrom(other.getClass()) && key.getClass().isArray()) {
      Object otherKey = ((play.db.Model) other)._key();
      if (otherKey.getClass().isArray()) {
        return Arrays.deepEquals((Object[]) key, (Object[]) otherKey);
      }
      return false;
    }

    if (!this.getClass().isAssignableFrom(other.getClass())) {
      return false;
    }

    return key.equals(((play.db.Model) other)._key());
  }

  @Override
  public int hashCode() {
    Object key = this._key();
    if (key == null) {
      return 0;
    }
    if (key.getClass().isArray()) {
      return Arrays.deepHashCode((Object[]) key);
    }
    return key.hashCode();
  }

  @Override
  public String toString() {
    Object key = this._key();
    StringBuilder keyStr = new StringBuilder(64).append(getClass().getSimpleName()).append("[");
    if (key != null && key.getClass().isArray()) {
      for (Object object : (Object[]) key) {
        keyStr.append(object).append(", ");
      }
      keyStr.setLength(keyStr.length() - 2);
    } else if (key != null) {
      keyStr.append(key);
    }
    keyStr.append("]");
    return keyStr.toString();
  }

  public static class JPAQueryException extends RuntimeException {

    public JPAQueryException(String message) {
      super(message);
    }

    public JPAQueryException(String message, Throwable e) {
      super(message + ": " + e.getMessage(), e);
    }

    public static Throwable findBestCause(Throwable e) {
      Throwable best = e;
      Throwable cause = e;
      int it = 0;
      while ((cause = cause.getCause()) != null && it++ < 10) {
        if (cause instanceof ClassCastException) {
          best = cause;
          break;
        }
        if (cause instanceof SQLException) {
          best = cause;
          break;
        }
      }
      return best;
    }
  }
}
