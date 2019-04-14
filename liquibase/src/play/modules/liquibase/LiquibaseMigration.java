package play.modules.liquibase;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.lockservice.LockServiceFactory;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public final class LiquibaseMigration {

  private static final Logger logger = LoggerFactory.getLogger(LiquibaseMigration.class);

  private final String dbName;
  private final String changeLogPath;
  private final File dumpFile;
  private final String driver;
  private final String url;
  private final String username;
  private final String password;

  public LiquibaseMigration(String dbName, String changeLogPath, String dumpFile, String driver, String url, String username, String password) {
    this.dbName = dbName;
    this.changeLogPath = changeLogPath;
    this.dumpFile = new File(dumpFile);
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
      if (isH2()) {
        restoreFromDump(cnx);
        LockServiceFactory.getInstance().register(new NonLockingLockService());
      }

      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(cnx));
      try {
        ResourceAccessor accessor = new DuplicatesIgnoringResourceAccessor(Thread.currentThread().getContextClassLoader());
        Liquibase liquibase = new Liquibase(changeLogPath, accessor, database);
        liquibase.update(Play.configuration.getProperty("liquibase.contexts", ""));
        if (isH2()) {
          storeDump(cnx);
        }
      }
      finally {
        close(database);
      }

      logger.info("{} finished in {} ms.", changeLogPath, NANOSECONDS.toMillis(nanoTime() - start));
    }
    catch (SQLException | LiquibaseException sqe) {
      throw new LiquibaseUpdateException("Failed to migrate " + changeLogPath, sqe);
    }
  }

  private boolean isH2() {
    return url.contains(":h2:");
  }

  private void storeDump(Connection cnx) {
    logger.info("Store {} DB to {}", dbName, dumpFile);
    try (Statement statement = cnx.createStatement()) {
      String sql = String.format("script nopasswords to '%s'", dumpFile);
      statement.execute(sql);
    }
    catch (SQLException ex) {
      throw new LiquibaseUpdateException(String.format("Failed to store %s DB dump to %s", dbName, dumpFile), ex);
    }
  }

  private void restoreFromDump(Connection cnx) {
    if (dumpFile.exists()) {
      logger.info("Restore {} DB from {}", dbName, dumpFile);
      try (Statement statement = cnx.createStatement()) {
        String sql = String.format("runscript from '%s'", dumpFile);
        statement.execute(sql);
      }
      catch (SQLException ex) {
        throw new LiquibaseUpdateException(String.format("Failed to restore %s DB from dump %s", dbName, dumpFile), ex);
      }
    }
    else {
      logger.info("{} DB dump {} not found, creating DB from scratch", dbName, dumpFile);
    }
  }

  private void close(Database database) {
    try {
      database.close();
    }
    catch (DatabaseException | RuntimeException e) {
      logger.warn("{} problem closing connection: " + e, changeLogPath, e);
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
      Driver d = (Driver) Class.forName(driver).getConstructor().newInstance();
      DriverManager.registerDriver(d);
    } catch (Exception e) {
      throw new LiquibaseUpdateException("jdbc driver class not found: " + driver, e);
    }
  }
}
