package play.db.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.PersistenceUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.InvocationContext;
import play.exceptions.JPAException;
import play.libs.SupplierWithException;

/** JPA Support */
public class JPA {
  private static final Logger logger = LoggerFactory.getLogger(JPA.class);

  protected static Map<String, EntityManagerFactory> emfs = new ConcurrentHashMap<>();
  public static final ThreadLocal<Map<String, JPAContext>> currentEntityManager =
      ThreadLocal.withInitial(() -> new ConcurrentHashMap<>());
  public static String DEFAULT = "default";

  public static class JPAContext {
    public String dbName = JPA.DEFAULT;
    public EntityManager entityManager;
    public boolean readonly = true;
    public boolean autoCommit;
  }

  public static boolean isInitialized() {
    return get(DEFAULT) != null;
  }

  static Map<String, JPAContext> get() {
    return currentEntityManager.get();
  }

  static JPAContext get(String name) {
    return get().get(name);
  }

  /**
   * Clear a DB context
   *
   * @param name the DB name
   */
  static void clearContext(String name) {
    get().remove(name);
  }

  static void createContext(String dbName, EntityManager entityManager, boolean readonly) {
    if (isInitialized()) {
      try {
        get(dbName).entityManager.close();
      } catch (Exception e) {
        // Let's it fail
      }
      clearContext(dbName);
    }
    bindForCurrentThread(dbName, entityManager, readonly);
  }

  /**
   * Get the EntityManager for specified persistence unit for this thread.
   *
   * @param key The DB name
   * @return The EntityManager
   */
  public static EntityManager em(String key) {
    JPAContext jpaContext = get(key);
    if (jpaContext == null)
      throw new JPAException(
          "No active EntityManager for name [" + key + "], transaction not started?");
    return jpaContext.entityManager;
  }

  /**
   * Bind an EntityManager to the current thread.
   *
   * @param name The DB name
   * @param em The EntityManager
   * @param readonly indicate if it is in read only mode
   */
  public static void bindForCurrentThread(String name, EntityManager em, boolean readonly) {
    JPAContext context = new JPAContext();
    context.dbName = name;
    context.entityManager = em;
    context.readonly = readonly;

    // Get all our context for our current thread
    get().put(name, context);
  }

  public static void unbindForCurrentThread(String name) {
    // Get all our context for our current thread
    get().remove(name);
  }

  // ~~~~~~~~~~~
  /*
   * Retrieve the current entityManager
   */
  public static EntityManager em() {
    return em(DEFAULT);
  }

  /** @return true if an entityManagerFactory has started */
  public static boolean isEnabled() {
    return isEnabled(DEFAULT);
  }

  public static boolean isEnabled(String em) {
    return emfs.get(em) != null;
  }

  /**
   * Execute a JPQL query
   *
   * @param query The query to execute
   * @return The result code
   */
  public static int execute(String query) {
    return execute(DEFAULT, query);
  }

  public static int execute(String em, String query) {
    return em(em).createQuery(query).executeUpdate();
  }

  /**
   * Build a new entityManager
   *
   * <p>(In most case you want to use the local entityManager with em)
   */
  public static EntityManager createEntityManager(String name) {
    if (isEnabled(name)) {
      return emfs.get(name).createEntityManager();
    }
    return null;
  }

  /** @return true if current thread is running inside a transaction */
  public static boolean isInsideTransaction() {
    return isInsideTransaction(DEFAULT);
  }

  public static boolean isInsideTransaction(String name) {
    JPAContext jpaContext = get(name);
    return jpaContext != null
        && jpaContext.entityManager != null
        && jpaContext.entityManager.getTransaction() != null;
  }

  public static <T> T withinFilter(SupplierWithException<T> block) throws Exception {
    if (InvocationContext.current().getAnnotation(NoTransaction.class) != null) {
      // Called method or class is annotated with @NoTransaction telling us that
      // we should not start a transaction
      return block.get();
    }

    boolean readOnly = false;
    String name = DEFAULT;
    Transactional tx = InvocationContext.current().getAnnotation(Transactional.class);
    if (tx != null) {
      readOnly = tx.readOnly();
    }
    PersistenceUnit pu = InvocationContext.current().getAnnotation(PersistenceUnit.class);
    if (pu != null) {
      name = pu.name();
    }

    return withTransaction(name, readOnly, block);
  }

  public static String getDBName(Class<?> clazz) {
    String name = JPA.DEFAULT;
    if (clazz != null) {
      PersistenceUnit pu = clazz.getAnnotation(PersistenceUnit.class);
      if (pu != null) {
        name = pu.name();
      }
    }
    return name;
  }

  /**
   * Run a block of code in a JPA transaction.
   *
   * @param dbName The persistence unit name
   * @param readOnly Is the transaction read-only?
   * @param block Block of code to execute.
   * @param <T> The entity class
   * @return The result
   */
  public static <T> T withTransaction(
      String dbName, boolean readOnly, SupplierWithException<T> block) throws Exception {
    if (isEnabled()) {
      boolean closeEm = true;
      // For each existing persistence unit

      try {
        // we are starting a transaction for all known persistent unit
        // this is probably not the best, but there is no way we can know where to go from
        // at this stage
        for (String name : emfs.keySet()) {
          EntityManager localEm = JPA.createEntityManager(name);
          JPA.bindForCurrentThread(name, localEm, readOnly);

          if (!readOnly) {
            localEm.getTransaction().begin();
          }
        }

        T result = block.get();

        boolean rollbackAll = false;
        // Get back our entity managers
        // Because people might have mess up with the current entity managers
        for (JPAContext jpaContext : get().values()) {
          EntityManager m = jpaContext.entityManager;
          EntityTransaction localTx = m.getTransaction();
          // The resource transaction must be in progress in order to determine if it has been marked for
          // rollback
          if (localTx.isActive() && localTx.getRollbackOnly()) {
            rollbackAll = true;
          }
        }

        for (JPAContext jpaContext : get().values()) {
          EntityManager m = jpaContext.entityManager;
          boolean ro = jpaContext.readonly;
          EntityTransaction localTx = m.getTransaction();
          // transaction must be active to make some rollback or commit
          if (localTx.isActive()) {
            if (rollbackAll || ro) {
              localTx.rollback();
            } else {
              localTx.commit();
            }
          }
        }

        return result;
      } catch (Exception t) {
        // Because people might have mess up with the current entity managers
        for (JPAContext jpaContext : get().values()) {
          EntityManager m = jpaContext.entityManager;
          EntityTransaction localTx = m.getTransaction();
          try {
            // transaction must be active to make some rollback or commit
            if (localTx.isActive()) {
              localTx.rollback();
            }
          } catch (Throwable e) {
            logger.error("Failed to rollback transaction", e);
          }
        }

        throw t;
      } finally {
        if (closeEm) {
          for (JPAContext jpaContext : get().values()) {
            EntityManager localEm = jpaContext.entityManager;
            if (localEm.isOpen()) {
              localEm.close();
            }
            JPA.clearContext(jpaContext.dbName);
          }
          for (String name : emfs.keySet()) {
            JPA.unbindForCurrentThread(name);
          }
        }
      }
    } else {
      return block.get();
    }
  }

  /**
   * initialize the JPA context and starts a JPA transaction
   *
   * @param name The persistence unit name
   * @param readOnly true for a readonly transaction
   */
  public static void startTx(String name, boolean readOnly) {
    EntityManager manager = createEntityManager(name);
    manager.setFlushMode(FlushModeType.COMMIT);
    manager.setProperty("org.hibernate.readOnly", readOnly);
    manager.getTransaction().begin();
    createContext(name, manager, readOnly);
  }

  public static void closeTx(String name) {
    if (JPA.isInsideTransaction(name)) {
      EntityManager manager = em(name);
      try {
        // Be sure to set the connection is non-autoCommit mode as some driver will complain about COMMIT
        // statement
        try {
          manager.unwrap(Session.class).doWork(con -> con.setAutoCommit(false));
        } catch (Exception e) {
          logger.error("Why the driver complains here?", e);
        }
        // Commit the transaction
        if (manager.getTransaction().isActive()) {
          if (JPA.get().get(name).readonly || manager.getTransaction().getRollbackOnly()) {
            manager.getTransaction().rollback();
          } else {
            manager.getTransaction().commit();
          }
        }
      } finally {
        if (manager.isOpen()) {
          manager.close();
        }
        JPA.clearContext(name);
      }
    }
  }

  public static void rollbackTx(String name) {
    if (JPA.isInsideTransaction()) {
      EntityManager manager = em(name);
      try {
        // Be sure to set the connection is non-autoCommit mode as some driver will complain about COMMIT
        // statement
        try {
          manager.unwrap(Session.class).doWork(con -> con.setAutoCommit(false));
        } catch (Exception e) {
          logger.error("Why the driver complains here?", e);
        }
        if (manager.getTransaction().isActive()) {
          manager.getTransaction().rollback();
        }

      } finally {
        if (manager.isOpen()) {
          manager.close();
        }
        JPA.clearContext(name);
      }
    }
  }
}
