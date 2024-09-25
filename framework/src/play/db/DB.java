package play.db;

import static java.util.stream.Collectors.toMap;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.jpa.JPA;
import play.exceptions.DatabaseException;

/** Database connection utilities. */
public class DB {

  private static final Logger logger = LoggerFactory.getLogger(DB.class);

  /**
   * The loaded datasource.
   *
   * @see ExtendedDatasource
   */
  protected static final Map<String, ExtendedDatasource> dataSources = new ConcurrentHashMap<>();

  public static class ExtendedDatasource {

    /** Connection to the physical data source */
    private final DataSource internalDataSource;

    /** The method used to destroy the data source */
    private final String internalDestroyMethod;

    public ExtendedDatasource(DataSource ds, String destroyMethod) {
      this.internalDataSource = ds;
      this.internalDestroyMethod = destroyMethod;
    }

    public String getDestroyMethod() {
      return internalDestroyMethod;
    }

    public DataSource getDataSource() {
      return internalDataSource;
    }
  }

  /**
   * @see #dataSources
   * @see ExtendedDatasource
   * @since 1.3.0
   * @deprecated Use dataSources instead
   */
  @Deprecated public static DataSource dataSource = null;

  /**
   * The method used to destroy the datasource
   *
   * @see #dataSources
   * @see ExtendedDatasource
   * @since 1.3.0
   * @deprecated Use dataSources instead
   */
  @Deprecated public static String destroyMethod = "";

  public static final String DEFAULT = "default";

  static final ThreadLocal<Map<String, Connection>> localConnection = new ThreadLocal<>();

  @Nullable
  public static DataSource getDataSource(@Nonnull String name) {
    if (dataSources.get(name) != null) {
      return dataSources.get(name).getDataSource();
    }
    return null;
  }

  @Nonnull
  @SuppressWarnings("ConstantConditions")
  public static DataSource getDataSource() {
    return getDataSource(DEFAULT);
  }

  @Nonnull
  public static Map<String, DataSource> getDataSources() {
    return dataSources
        .entrySet()
        .stream()
        .collect(toMap(e -> e.getKey(), e -> e.getValue().getDataSource()));
  }

  @Nullable
  private static Connection getLocalConnection(@Nonnull String name) {
    Map<String, Connection> map = localConnection.get();
    if (map != null) {
      return map.get(name);
    }
    return null;
  }

  private static void registerLocalConnection(String name, Connection connection) {
    Map<String, Connection> map = localConnection.get();
    if (map == null) {
      map = new HashMap<>();
    }
    map.put(name, connection);
    localConnection.set(map);
  }

  /** Close all the open connections for the current thread. */
  public static void closeAll() {
    Map<String, Connection> map = localConnection.get();
    if (map != null) {
      Set<String> keySet = new HashSet<>(map.keySet());
      for (String name : keySet) {
        close(name);
      }
    }
  }

  /** Close all the open connections for the current thread. */
  public static void close() {
    close(DEFAULT);
  }

  /**
   * Close a given open connections for the current thread
   *
   * @param name Name of the DB
   */
  public static void close(String name) {
    Map<String, Connection> map = localConnection.get();
    if (map != null) {
      Connection connection = map.get(name);
      if (connection != null) {
        map.remove(name);
        localConnection.set(map);
        try {
          connection.close();
        } catch (Exception e) {
          throw new DatabaseException(
              "It's possible than the connection '" + name + "'was not properly closed !", e);
        }
      }
    }
  }

  public static void execute(String SQL) {
    execute(DEFAULT, SQL);
  }

  public static void execute(String name, String sql) {
    JPA.em(name)
        .unwrap(Session.class)
        .doWork(
            con -> {
              try (Statement statement = con.createStatement()) {
                statement.execute(sql);
              } catch (SQLException ex) {
                throw new DatabaseException(ex);
              }
            });
  }

  /**
   * Destroy the datasource
   *
   * @param name the DB name
   */
  public static void destroy(String name) {
    try {
      ExtendedDatasource extDatasource = dataSources.get(name);
      if (extDatasource != null && extDatasource.getDestroyMethod() != null) {
        Method close =
            extDatasource.internalDataSource
                .getClass()
                .getMethod(extDatasource.getDestroyMethod());
        close.invoke(extDatasource.getDataSource());
        dataSources.remove(name);
        DB.dataSource = null;
        logger.trace("Datasource destroyed");
      }
    } catch (NoSuchMethodException t) {
      logger.debug("Couldn't destroy the datasource: {}", t.toString());
    } catch (Throwable t) {
      logger.error("Couldn't destroy the datasource", t);
    }
  }

  /** Destroy all dataSources */
  public static void destroyAll() {
    Set<String> keySet = new HashSet<>(dataSources.keySet());
    for (String name : keySet) {
      destroy(name);
    }
  }
}
