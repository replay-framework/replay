package play.modules.liquibase;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.PlayPlugin;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class LiquibasePlugin extends PlayPlugin {

  private static final Logger logger = LoggerFactory.getLogger(LiquibasePlugin.class);

  @Override
  public void onApplicationStart() {
    String autoUpdate = Play.configuration.getProperty("liquibase.active", "false");

    if (!parseBoolean(autoUpdate)) {
      logger.info("Auto update flag [{}] != true  => skipping structural update", autoUpdate);
      return;
    }

    long start = nanoTime();
    logger.info("Auto update flag found and positive => let's get on with changelog update");

    try (Connection cnx = getConnection()) {
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(cnx));

      try {
        Liquibase liquibase = createLiquibase(database);
        String contexts = parseContexts();
        liquibase.update(contexts);
      }
      finally {
        close(database);
      }
    }
    catch (SQLException | LiquibaseException sqe) {
      throw new LiquibaseUpdateException(sqe.getMessage(), sqe);
    }
    finally {
      logger.info("LiquibasePlugin finished with {} ms.", NANOSECONDS.toMillis(nanoTime() - start));
    }
  }

  private Liquibase createLiquibase(Database database) throws LiquibaseException {
    ResourceAccessor accessor = createResourceAccessor();

    String changeLogPath = Play.configuration.getProperty("liquibase.changelog", "mainchangelog.xml");
    Liquibase liquibase = new Liquibase(changeLogPath, accessor, database);

    configureLiquibaseProperties(liquibase);
    return liquibase;
  }

  private void close(Database database) {
    try {
      database.close();
    }
    catch (DatabaseException | RuntimeException e) {
      logger.warn("problem closing connection: " + e, e);
    }
  }

  @Nullable String parseContexts() {
    String contexts = Play.configuration.getProperty("liquibase.contexts", "").trim();
    return contexts.isEmpty() ? null : contexts;
  }

  private void configureLiquibaseProperties(Liquibase liquibase) {
    Properties props = Play.configuration;

    for (String name : props.stringPropertyNames()) {
      if (name.startsWith("db.") || name.startsWith("liquibase.")) {
        String val = props.getProperty(name);
        liquibase.setChangeLogParameter(name, val);
      }
    }
  }

  private ResourceAccessor createResourceAccessor() {
    String scanner = Play.configuration.getProperty("liquibase.scanner", "jar");

    switch (scanner) {
      case "jar":
        return new DuplicatesIgnoringResourceAccessor(Thread.currentThread().getContextClassLoader());
      case "src":
        return new FileSystemResourceAccessor(Play.applicationPath.getAbsolutePath());
      default:
        throw new LiquibaseUpdateException("No valid scanner found liquibase operation " + scanner);
    }
  }

  @SuppressWarnings("CallToDriverManagerGetConnection")
  private Connection getConnection() throws SQLException {
    String driver = Play.configuration.getProperty("db.driver");
    String url = Play.configuration.getProperty("liquibase.db.url", Play.configuration.getProperty("db.url"));
    String username = Play.configuration.getProperty("liquibase.db.user", Play.configuration.getProperty("db.user"));
    String password = Play.configuration.getProperty("liquibase.db.pass", Play.configuration.getProperty("db.pass"));
    logger.info("Migrate DB: {} @ {}", username, url);

    initDriver(driver);
    return DriverManager.getConnection(url, username, password);
  }

  private void initDriver(String driver) {
    try {
      Driver d = (Driver) Class.forName(driver).newInstance();
      DriverManager.registerDriver(d);
    } catch (Exception e) {
      throw new RuntimeException("jdbc driver class not found", e);
    }
  }
}
