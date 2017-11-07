package play.modules.liquibase;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.ValidationFailedException;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.PlayPlugin;
import play.utils.Properties;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Boolean.parseBoolean;

public class LiquibasePlugin extends PlayPlugin {

  private static final Logger logger = LoggerFactory.getLogger(LiquibasePlugin.class);

  @Override
  public void onApplicationStart() {
    String autoUpdate = Play.configuration.getProperty("liquibase.active", "false");

    if (!parseBoolean(autoUpdate)) {
      logger.info("Auto update flag [{}] != true  => skipping structural update", autoUpdate);
      return;
    }

    logger.info("Auto update flag found and positive => let's get on with changelog update");

    try (Connection cnx = getConnection()) {
      Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(cnx));

      try {
        Liquibase liquibase = createLiquibase(database);
        String contexts = parseContexts();

        for (LiquibaseAction op : parseLiquibaseActions()) {
          performAction(liquibase, op, contexts);
        }
      }
      finally {
        close(database);
      }
    }
    catch (SQLException | LiquibaseException | IOException sqe) {
      throw new LiquibaseUpdateException(sqe.getMessage(), sqe);
    }
  }

  private void performAction(Liquibase liquibase, LiquibaseAction op, String contexts) throws LiquibaseException, IOException {
    logger.info("Dealing with op [{}]", op);

    switch (op) {
      case LISTLOCKS:
        liquibase.reportLocks(System.out);
        break;
      case RELEASELOCKS:
        liquibase.forceReleaseLocks();
        break;
      case SYNC:
        liquibase.changeLogSync(contexts);
        break;
      case STATUS:
        File tmp = Play.tmpDir.createTempFile("liquibase", ".status");
        try (Writer out = new BufferedWriter(new FileWriter(tmp))) {
          liquibase.reportStatus(true, contexts, out);
        }
        logger.info("status dumped into file [{}]", tmp.getAbsolutePath());
        break;
      case UPDATE:
        liquibase.update(contexts);
        break;
      case CLEARCHECKSUMS:
        liquibase.clearCheckSums();
        break;
      case DROPALL:
        liquibase.dropAll();
        break;
      case VALIDATE:
        try {
          liquibase.validate();
        }
        catch (ValidationFailedException e) {
          logger.error("liquibase validation error", e);
        }
        break;
    }
    logger.info("op [{}] performed", op);
  }

  private Liquibase createLiquibase(Database database) throws LiquibaseException, IOException {
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

  String parseContexts() {
    String contexts = Play.configuration.getProperty("liquibase.contexts", "").trim();
    return contexts.isEmpty() ? null : contexts;
  }

  private void configureLiquibaseProperties(Liquibase liquibase) throws IOException {
    String propertiesPath = Play.configuration.getProperty("liquibase.properties", "liquibase.properties");
    try (InputStream in = readPropertiesFile(propertiesPath)) {
      if (in != null) {
        Properties props = new Properties();
        props.load(in);

        for (Map.Entry<String, String> stringStringEntry : props.entrySet()) {
          String val = stringStringEntry.getValue();
          logger.info("found parameter [{}] / [{}] for liquibase update", stringStringEntry.getKey(), val);
          liquibase.setChangeLogParameter(stringStringEntry.getKey(), val);
        }
      }
      else {
        logger.info("Could not find properties file [{}]", propertiesPath);
      }
    }
  }

  private InputStream readPropertiesFile(String propertiesPath) throws FileNotFoundException {
    String scanner = Play.configuration.getProperty("liquibase.scanner", "jar");
    switch (scanner) {
      case "jar":
        return Play.classloader.getResourceAsStream(propertiesPath);
      default:
        return new FileInputStream(Play.getFile(propertiesPath));
    }
  }

  private ResourceAccessor createResourceAccessor() {
    String scanner = Play.configuration.getProperty("liquibase.scanner", "jar");

    switch (scanner) {
      case "jar":
        return new DuplicatesIgnoringResourceAccessor(Play.classloader);
      case "src":
        return new FileSystemResourceAccessor(Play.applicationPath.getAbsolutePath());
      default:
        throw new LiquibaseUpdateException("No valid scanner found liquibase operation " + scanner);
    }
  }

  List<LiquibaseAction> parseLiquibaseActions() {
    String liquibaseActions = Play.configuration.getProperty("liquibase.actions", "");
    if (liquibaseActions == null || liquibaseActions.isEmpty()) {
      throw new LiquibaseUpdateException("No valid action found for liquibase operation. Please set property 'liquibase.actions'.");
    }

    List<LiquibaseAction> actions = new ArrayList<>();
    for (String action : liquibaseActions.split(",")) {
      LiquibaseAction op = LiquibaseAction.valueOf(action.toUpperCase());
      actions.add(op);
    }

    return actions;
  }

  @SuppressWarnings("CallToDriverManagerGetConnection")
  private Connection getConnection() throws SQLException {
    String url = Play.configuration.getProperty("db.url");
    String username = Play.configuration.getProperty("db.user");
    String password = Play.configuration.getProperty("db.pass");
    logger.info("Migrate DB: {} @ {}", username, url);
    return DriverManager.getConnection(url, username, password);
  }
}
