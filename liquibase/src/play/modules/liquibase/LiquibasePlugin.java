package play.modules.liquibase;

import play.Play;
import play.PlayPlugin;

public class LiquibasePlugin extends PlayPlugin {
  @Override
  public void onApplicationStart() {
    String driver = Play.configuration.getProperty("db.driver");
    String url = Play.configuration.getProperty("db.url");
    String username = Play.configuration.getProperty("db.user");
    String password = Play.configuration.getProperty("db.pass");
    String changeLogPath =
        Play.configuration.getProperty("liquibase.changelog", "mainchangelog.xml");

    new LiquibaseMigration(Play.id, "default", changeLogPath, driver, url, username, password)
        .migrate();
  }
}
