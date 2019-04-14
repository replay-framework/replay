package play.modules.liquibase;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public final class LiquibaseMigration {

  private static final Logger logger = LoggerFactory.getLogger(LiquibaseMigration.class);

  private final String changeLogPath;
  private final String driver;
  private final String url;
  private final String username;
  private final String password;

  public LiquibaseMigration(String changeLogPath, String driver, String url, String username, String password) {
    this.changeLogPath = changeLogPath;
    this.driver = driver;
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public void migrate() {
    String autoUpdate = Play.configuration.getProperty("liquibase.active", "false");

    if (!parseBoolean(autoUpdate)) {
      logger.info("{} Auto update flag [{}] != true  => skipping structural update", changeLogPath, autoUpdate);
      return;
    }

    long start = nanoTime();

    try (Connection cnx = getConnection()) {
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(cnx));

      try {
        Liquibase liquibase = createLiquibase(database, changeLogPath);
        liquibase.update(Play.configuration.getProperty("liquibase.contexts", ""));
      }
      finally {
        close(database);
      }
    }
    catch (SQLException | LiquibaseException sqe) {
      throw new LiquibaseUpdateException("Failed to migrate " + changeLogPath, sqe);
    }
    finally {
      logger.info("{} finished in {} ms.", changeLogPath, NANOSECONDS.toMillis(nanoTime() - start));
    }
  }

  private Liquibase createLiquibase(Database database, String changeLogPath) throws LiquibaseException {
    ResourceAccessor accessor = new DuplicatesIgnoringResourceAccessor(Thread.currentThread().getContextClassLoader());

    Liquibase liquibase = new Liquibase(changeLogPath, accessor, database);

    configureLiquibaseProperties(liquibase);
    return liquibase;
  }

  private void close(Database database) {
    try {
      database.close();
    }
    catch (DatabaseException | RuntimeException e) {
      logger.warn("{} problem closing connection: " + e, changeLogPath, e);
    }
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

  @SuppressWarnings("CallToDriverManagerGetConnection")
  private Connection getConnection() throws SQLException {
    logger.info("Migrate {}: {} @ {}", changeLogPath, username, url);

    initDriver(driver);
    return DriverManager.getConnection(url, username, password);
  }

  private void initDriver(String driver) {
    try {
      Driver d = (Driver) Class.forName(driver).newInstance();
      DriverManager.registerDriver(d);
    } catch (Exception e) {
      throw new LiquibaseUpdateException("jdbc driver class not found: " + driver, e);
    }
  }
}
