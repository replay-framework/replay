package play.db.jpa;

import java.io.Serializable;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.type.Type;

public class HibernateInterceptor extends EmptyInterceptor {

  public HibernateInterceptor() {}

  @Override
  public int[] findDirty(
      Object o, Serializable id, Object[] arg2, Object[] arg3, String[] arg4, Type[] arg5) {
    if (o instanceof JPABase && !((JPABase) o).willBeSaved) {
      return new int[0];
    }
    return null;
  }

  @Override
  public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
    if (collection instanceof PersistentCollection) {
      Object o = ((PersistentCollection) collection).getOwner();
      if (o instanceof JPABase) {
        if (entities.get() != null) {
          return;
        } else {
          return;
        }
      }
    } else {
      System.out.println("HOO: Case not handled !!!");
    }
    super.onCollectionUpdate(collection, key);
  }

  @Override
  public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
    if (collection instanceof PersistentCollection) {
      Object o = ((PersistentCollection) collection).getOwner();
      if (o instanceof JPABase) {
        if (entities.get() != null) {
          return;
        } else {
          return;
        }
      }
    } else {
      System.out.println("HOO: Case not handled !!!");
    }

    super.onCollectionRecreate(collection, key);
  }

  @Override
  public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
    if (collection instanceof PersistentCollection) {
      Object o = ((PersistentCollection) collection).getOwner();
      if (o instanceof JPABase) {
        if (entities.get() != null) {
          return;
        } else {
          return;
        }
      }
    } else {
      System.out.println("HOO: Case not handled !!!");
    }
    super.onCollectionRemove(collection, key);
  }

  protected final ThreadLocal<Object> entities = new ThreadLocal<>();

  @Override
  public boolean onSave(
      Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
    entities.set(entity);
    return super.onSave(entity, id, state, propertyNames, types);
  }

  @Override
  public void afterTransactionCompletion(org.hibernate.Transaction tx) {
    entities.remove();
  }
}
