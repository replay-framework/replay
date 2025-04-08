package play.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.ConfProperties;
import play.Play;

public class ConfigurationTest {

  @BeforeEach
  public void setUp() {
    Play.configuration = new ConfProperties();
  }

  @Test
  public void dbNameResolver_singleDatabase() {
    Play.configuration.put("db", "mysql:user:pwd@database_name");
    Set<String> dbNames = Configuration.getDbNames();
    assertThat(dbNames.size()).isEqualTo(1);
    assertThat(dbNames.iterator().next()).isEqualTo("default");
  }

  @Test
  public void dbNameResolver_multipleDatabases() {
    Play.configuration.put("db", "mysql:user:pwd@database_name");
    Play.configuration.put("db.test", "mysql:user:pwd@database_name2");
    List<String> dbNames = new ArrayList<>(Configuration.getDbNames());
    assertThat(dbNames.size()).isEqualTo(2);
    assertThat(dbNames.get(0)).isEqualTo("default");
    assertThat(dbNames.get(1)).isEqualTo("test");
    assertThat(new Configuration("default").getProperty("db"))
        .isEqualTo("mysql:user:pwd@database_name");
    assertThat(new Configuration("test").getProperty("db"))
        .isEqualTo("mysql:user:pwd@database_name2");
  }

  @Test
  public void dbNameResolverMultiDbURL_singleDatabase() {
    Play.configuration.put("db.url", "jdbc:postgresql://localhost/database_name");
    Play.configuration.put("db.driver", "org.postgresql.Driver");
    Play.configuration.put("db.user", "user");
    Play.configuration.put("db.pass", "pass");

    Set<String> dbNames = Configuration.getDbNames();
    assertThat(dbNames.size()).isEqualTo(1);
    assertThat(dbNames.iterator().next()).isEqualTo("default");
  }

  @Test
  public void dbNameResolverMultiDbURL_multipleDatabases() {
    Play.configuration.put("db.url", "jdbc:postgresql://localhost/database_name");
    Play.configuration.put("db.driver", "org.postgresql.Driver");
    Play.configuration.put("db.user", "user");
    Play.configuration.put("db.pass", "pass");
    Play.configuration.put("db.test.url", "jdbc:postgresql://localhost/database_name2");
    Play.configuration.put("db.test.driver", "org.postgresql.Driver");
    Play.configuration.put("db.test.user", "user2");
    Play.configuration.put("db.test.pass", "pass2");

    List<String> dbNames = new ArrayList<>(Configuration.getDbNames());
    assertThat(dbNames.size()).isEqualTo(2);
    assertThat(dbNames.get(0)).isEqualTo("default");
    assertThat(dbNames.get(1)).isEqualTo("test");

    Configuration configuration1 = new Configuration("default");
    assertThat(configuration1.getProperty("db.url"))
        .isEqualTo("jdbc:postgresql://localhost/database_name");
    assertThat(configuration1.getProperty("db.driver")).isEqualTo("org.postgresql.Driver");
    assertThat(configuration1.getProperty("db.user")).isEqualTo("user");
    assertThat(configuration1.getProperty("db.pass")).isEqualTo("pass");

    Configuration configuration2 = new Configuration("test");
    assertThat(configuration2.getProperty("db.url"))
        .isEqualTo("jdbc:postgresql://localhost/database_name2");
    assertThat(configuration2.getProperty("db.driver")).isEqualTo("org.postgresql.Driver");
    assertThat(configuration2.getProperty("db.user")).isEqualTo("user2");
    assertThat(configuration2.getProperty("db.pass")).isEqualTo("pass2");
  }

  @Test
  public void dbNameResolverMySQLWithPoolTest() {
    Play.configuration = new ConfProperties();
    Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
    Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
    Play.configuration.put("db.user", "root");
    Play.configuration.put("db.pass", "");
    Play.configuration.put("db.pool.timeout", "1000");
    Play.configuration.put("db.pool.maxSize", "20");
    Play.configuration.put("db.pool.minSize", "1");
    Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

    Set<String> dbNames = Configuration.getDbNames();
    assertThat(dbNames.size()).isEqualTo(1);
    assertThat(dbNames.iterator().next()).isEqualTo("default");

    Configuration configuration = new Configuration("default");
    assertThat(configuration.getProperty("db.url")).isEqualTo("jdbc:mysql://127.0.0.1/testPlay");
    assertThat(configuration.getProperty("db.driver")).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(configuration.getProperty("db.user")).isEqualTo("root");
    assertThat(configuration.getProperty("db.pass")).isEqualTo("");
    assertThat(configuration.getProperty("db.pool.timeout")).isEqualTo("1000");
    assertThat(configuration.getProperty("db.pool.maxSize")).isEqualTo("20");
    assertThat(configuration.getProperty("db.pool.minSize")).isEqualTo("1");
    assertThat(configuration.getProperty("db.pool.maxIdleTimeExcessConnections")).isEqualTo("60");
  }

  @Test
  public void dbNameResolverMySQLWithPoolAndDBConflictTest() {
    Play.configuration = new ConfProperties();

    Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
    Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
    Play.configuration.put("db.user", "root");
    Play.configuration.put("db.pass", "");
    Play.configuration.put("db.pool.timeout", "1000");
    Play.configuration.put("db.pool.maxSize", "20");
    Play.configuration.put("db.pool.minSize", "1");
    Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

    Play.configuration.put("db", "mysql:user:pwd@database_name");
    Set<String> dbNames = Configuration.getDbNames();
    assertThat(dbNames.size()).isEqualTo(1);
    assertThat(dbNames.iterator().next()).isEqualTo("default");
  }

  @Test
  public void dbNameResolverMySQLWithPoolAndDBTest() {
    Play.configuration = new ConfProperties();

    Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
    Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
    Play.configuration.put("db.user", "root");
    Play.configuration.put("db.pass", "");
    Play.configuration.put("db.pool.timeout", "1000");
    Play.configuration.put("db.pool.maxSize", "20");
    Play.configuration.put("db.pool.minSize", "1");
    Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

    Play.configuration.put("db.test", "mysql:user:pwd@database_name");

    Set<String> dbNames = Configuration.getDbNames();
    assertThat(dbNames.size()).isEqualTo(2);
    Iterator<String> it = dbNames.iterator();
    assertThat(it.next()).isEqualTo("default");
    assertThat(it.next()).isEqualTo("test");

    Configuration configuration = new Configuration("default");
    assertThat(configuration.getProperty("db.url")).isEqualTo("jdbc:mysql://127.0.0.1/testPlay");
    assertThat(configuration.getProperty("db.driver")).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(configuration.getProperty("db.user")).isEqualTo("root");
    assertThat(configuration.getProperty("db.pass")).isEqualTo("");
    assertThat(configuration.getProperty("db.pool.timeout")).isEqualTo("1000");
    assertThat(configuration.getProperty("db.pool.maxSize")).isEqualTo("20");
    assertThat(configuration.getProperty("db.pool.minSize")).isEqualTo("1");
    assertThat(configuration.getProperty("db.pool.maxIdleTimeExcessConnections")).isEqualTo("60");

    configuration = new Configuration("test");
    assertThat(configuration.getProperty("db")).isEqualTo("mysql:user:pwd@database_name");
    assertThat(configuration.getProperty("db.url")).isNull();
    assertThat(configuration.getProperty("db.driver")).isNull();
    assertThat(configuration.getProperty("db.user")).isNull();
    assertThat(configuration.getProperty("db.pass")).isNull();
    assertThat(configuration.getProperty("db.pool.timeout")).isNull();
    assertThat(configuration.getProperty("db.pool.maxSize")).isNull();
    assertThat(configuration.getProperty("db.pool.minSize")).isNull();
    assertThat(configuration.getProperty("db.pool.maxIdleTimeExcessConnections")).isNull();
  }

  @Test
  public void dbNameResolverMySQLWithPoolAndDBTest2() {
    Play.configuration = new ConfProperties();

    Play.configuration.put("db.test.url", "jdbc:mysql://127.0.0.1/testPlay");
    Play.configuration.put("db.test.driver", "com.mysql.jdbc.Driver");
    Play.configuration.put("db.test.user", "root");
    Play.configuration.put("db.test.pass", "");
    Play.configuration.put("db.test.pool.timeout", "1000");
    Play.configuration.put("db.test.pool.maxSize", "20");
    Play.configuration.put("db.test.pool.minSize", "1");
    Play.configuration.put("db.test.pool.maxIdleTimeExcessConnections", "60");

    Play.configuration.put("db", "mysql:user:pwd@database_name");

    Set<String> dbNames = Configuration.getDbNames();
    assertThat(dbNames.size()).isEqualTo(2);
    Iterator<String> it = dbNames.iterator();
    assertThat(it.next()).isEqualTo("default");
    assertThat(it.next()).isEqualTo("test");

    Configuration configuration = new Configuration("test");
    assertThat(configuration.getProperty("db")).isNull();
    assertThat(configuration.getProperty("db.url")).isEqualTo("jdbc:mysql://127.0.0.1/testPlay");
    assertThat(configuration.getProperty("db.driver")).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(configuration.getProperty("db.user")).isEqualTo("root");
    assertThat(configuration.getProperty("db.pass")).isEqualTo("");
    assertThat(configuration.getProperty("db.pool.timeout")).isEqualTo("1000");
    assertThat(configuration.getProperty("db.pool.maxSize")).isEqualTo("20");
    assertThat(configuration.getProperty("db.pool.minSize")).isEqualTo("1");
    assertThat(configuration.getProperty("db.pool.maxIdleTimeExcessConnections")).isEqualTo("60");

    configuration = new Configuration("default");
    assertThat(configuration.getProperty("db")).isEqualTo("mysql:user:pwd@database_name");
    assertThat(configuration.getProperty("db.url")).isNull();
    assertThat(configuration.getProperty("db.driver")).isNull();
    assertThat(configuration.getProperty("db.user")).isNull();
    assertThat(configuration.getProperty("db.pass")).isNull();
    assertThat(configuration.getProperty("db.pool.timeout")).isNull();
    assertThat(configuration.getProperty("db.pool.maxSize")).isNull();
    assertThat(configuration.getProperty("db.pool.minSize")).isNull();
    assertThat(configuration.getProperty("db.pool.maxIdleTimeExcessConnections")).isNull();
  }

  @Test
  public void convertToMultiDBTest() {
    Play.configuration = new ConfProperties();

    Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
    Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
    Play.configuration.put("db.user", "root");
    Play.configuration.put("db.pass", "");
    Play.configuration.put("db.pool.timeout", "1000");
    Play.configuration.put("db.pool.maxSize", "20");
    Play.configuration.put("db.pool.minSize", "1");
    Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");

    Configuration dbConfig = new Configuration("default");

    assertThat(dbConfig.getProperty("db.url")).isEqualTo("jdbc:mysql://127.0.0.1/testPlay");
    assertThat(dbConfig.getProperty("db.driver")).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(dbConfig.getProperty("db.user")).isEqualTo("root");
    assertThat(dbConfig.getProperty("db.pass")).isEqualTo("");
    assertThat(dbConfig.getProperty("db.pool.timeout")).isEqualTo("1000");
    assertThat(dbConfig.getProperty("db.pool.maxSize")).isEqualTo("20");
    assertThat(dbConfig.getProperty("db.pool.minSize")).isEqualTo("1");
    assertThat(dbConfig.getProperty("db.pool.maxIdleTimeExcessConnections")).isEqualTo("60");

    Set<String> dbNames = Configuration.getDbNames();
    assertThat(dbNames.size()).isEqualTo(1);
    Iterator<String> it = dbNames.iterator();
    assertThat(it.next()).isEqualTo("default");
  }

  @Test
  public void getPropertiesForDefaultTest() {
    //db
    Play.configuration.put("db.url", "jdbc:mysql://127.0.0.1/testPlay");
    Play.configuration.put("db.driver", "com.mysql.jdbc.Driver");
    Play.configuration.put("db.user", "root");
    Play.configuration.put("db.pass", "");
    Play.configuration.put("db.pool.timeout", "1000");
    Play.configuration.put("db.pool.maxSize", "20");
    Play.configuration.put("db.pool.minSize", "1");
    Play.configuration.put("db.pool.maxIdleTimeExcessConnections", "60");
    //jakarta.persistence
    Play.configuration.put("jakarta.persistence.lock.scope", "EXTENDED");
    Play.configuration.put("jakarta.persistence.lock.timeout", "1000");
    //jpa
    Play.configuration.put("jpa.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    Play.configuration.put("jpa.debugSQL", "true");
    //hibernate
    Play.configuration.put("hibernate.event.post-insert", "postInsert");
    Play.configuration.put("hibernate.event.post-update", "postUpdate");
    //org.hibernate
    Play.configuration.put("org.hibernate.flushMode", "AUTO");
    Play.configuration.put("hibernate.default_batch_fetch_size", "66");

    Configuration dbConfig = new Configuration("default");
    Map<String, String> properties = dbConfig.getProperties();

    //db
    assertThat(properties.get("db.url")).isEqualTo("jdbc:mysql://127.0.0.1/testPlay");
    assertThat(properties.get("db.driver")).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(properties.get("db.user")).isEqualTo("root");
    assertThat(properties.get("db.pass")).isEqualTo("");
    assertThat(properties.get("db.pool.timeout")).isEqualTo("1000");
    assertThat(properties.get("db.pool.maxSize")).isEqualTo("20");
    assertThat(properties.get("db.pool.minSize")).isEqualTo("1");
    assertThat(properties.get("db.pool.maxIdleTimeExcessConnections")).isEqualTo("60");
    //jakarta.persistence
    assertThat(properties.get("jakarta.persistence.lock.scope")).isEqualTo("EXTENDED");
    assertThat(properties.get("jakarta.persistence.lock.timeout")).isEqualTo("1000");
    //jpa
    assertThat(properties.get("jpa.dialect")).isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
    assertThat(properties.get("jpa.debugSQL")).isEqualTo("true");
    //hibernate
    assertThat(properties.get("hibernate.event.post-insert")).isEqualTo("postInsert");
    assertThat(properties.get("hibernate.event.post-update")).isEqualTo("postUpdate");
    //org.hibernate
    assertThat(properties.get("org.hibernate.flushMode")).isEqualTo("AUTO");
    assertThat(properties.get("hibernate.default_batch_fetch_size")).isEqualTo("66");

    assertThat(properties.size()).isEqualTo(Play.configuration.size());
  }

  @Test
  public void getPropertiesForDBTest() {
    //db
    Play.configuration.put("db.test.url", "jdbc:mysql://127.0.0.1/testPlay");
    Play.configuration.put("db.test.driver", "com.mysql.jdbc.Driver");
    Play.configuration.put("db.test.user", "root");
    Play.configuration.put("db.test.pass", "");
    Play.configuration.put("db.test.pool.timeout", "1000");
    Play.configuration.put("db.test.pool.maxSize", "20");
    Play.configuration.put("db.test.pool.minSize", "1");
    Play.configuration.put("db.test.pool.maxIdleTimeExcessConnections", "60");
    //jakarta.persistence
    Play.configuration.put("jakarta.persistence.test.lock.scope", "EXTENDED");
    Play.configuration.put("jakarta.persistence.test.lock.timeout", "1000");
    //jpa
    Play.configuration.put("jpa.test.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    Play.configuration.put("jpa.test.ddl", "update");
    Play.configuration.put("jpa.test.debugSQL", "true");
    //hibernate
    Play.configuration.put("hibernate.test.event.post-insert", "postInsert");
    Play.configuration.put("hibernate.test.event.post-update", "postUpdate");
    //org.hibernate
    Play.configuration.put("org.hibernate.test.flushMode", "AUTO");

    Configuration dbConfig = new Configuration("test");
    Map<String, String> properties = dbConfig.getProperties();

    //db
    assertThat(properties.get("db.url")).isEqualTo("jdbc:mysql://127.0.0.1/testPlay");
    assertThat(properties.get("db.driver")).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(properties.get("db.user")).isEqualTo("root");
    assertThat(properties.get("db.pass")).isEqualTo("");
    assertThat(properties.get("db.pool.timeout")).isEqualTo("1000");
    assertThat(properties.get("db.pool.maxSize")).isEqualTo("20");
    assertThat(properties.get("db.pool.minSize")).isEqualTo("1");
    assertThat(properties.get("db.pool.maxIdleTimeExcessConnections")).isEqualTo("60");
    //jakarta.persistence
    assertThat(properties.get("jakarta.persistence.lock.scope")).isEqualTo("EXTENDED");
    assertThat(properties.get("jakarta.persistence.lock.timeout")).isEqualTo("1000");
    //jpa
    assertThat(properties.get("jpa.dialect")).isEqualTo("org.hibernate.dialect.PostgreSQLDialect");
    assertThat(properties.get("jpa.debugSQL")).isEqualTo("true");
    //hibernate
    assertThat(properties.get("hibernate.event.post-insert")).isEqualTo("postInsert");
    assertThat(properties.get("hibernate.event.post-update")).isEqualTo("postUpdate");
    //org.hibernate
    assertThat(properties.get("org.hibernate.flushMode")).isEqualTo("AUTO");

    assertThat(properties.size()).isEqualTo(Play.configuration.size());
  }

  @Test
  public void generatesConfigurationPropertyNameBasedOnDatabaseName() {
    Configuration configuration = new Configuration("another");
    assertThat(configuration.generateKey("db")).isEqualTo("db.another");
    assertThat(configuration.generateKey("db.driver")).isEqualTo("db.another.driver");
    assertThat(configuration.generateKey("db.url")).isEqualTo("db.another.url");
    assertThat(configuration.generateKey("another-property")).isEqualTo("another-property");
  }

  @Test
  public void usesDefaultConfigurationPropertyNameForDefaultDatabase() {
    Configuration configuration = new Configuration("default");
    assertThat(configuration.generateKey("db")).isEqualTo("db.default");
    assertThat(configuration.generateKey("db.driver")).isEqualTo("db.default.driver");
    assertThat(configuration.generateKey("db.url")).isEqualTo("db.default.url");
    assertThat(configuration.generateKey("another-property")).isEqualTo("another-property");
  }

  @Test
  public void putPropertyToDefaultConfiguration() {
    Configuration configuration = new Configuration("default");
    configuration.put("db.driver", "org.h2.Driver");
    assertThat(configuration.getProperty("db.driver")).isEqualTo("org.h2.Driver");
    assertThat(Play.configuration.getProperty("db.default.driver")).isEqualTo("org.h2.Driver");
  }

  @Test
  public void putPropertyToCustomConfiguration() {
    Configuration configuration = new Configuration("custom");

    configuration.put("db.driver", "com.oracle.OracleDriver");
    assertThat(configuration.getProperty("db.driver")).isEqualTo("com.oracle.OracleDriver");
  }

  @Test
  public void putPropertyToMultipleCustomConfigurations() {
    Configuration configuration = new Configuration("default");
    Configuration configuration1 = new Configuration("db1");
    Configuration configuration2 = new Configuration("db2");

    configuration.put("db.driver", "com.oracle.OracleDriver");
    configuration1.put("db.driver", "org.h2.Driver");
    configuration2.put("db.driver", "com.mysql.Driver");

    assertThat(configuration.getProperty("db.driver")).isEqualTo("com.oracle.OracleDriver");
    assertThat(configuration1.getProperty("db.driver")).isEqualTo("org.h2.Driver");
    assertThat(configuration2.getProperty("db.driver")).isEqualTo("com.mysql.Driver");

    assertThat(Play.configuration.getProperty("db.default.driver"))
        .isEqualTo("com.oracle.OracleDriver");
    assertThat(Play.configuration.getProperty("db.db1.driver")).isEqualTo("org.h2.Driver");
    assertThat(Play.configuration.getProperty("db.db2.driver")).isEqualTo("com.mysql.Driver");
  }

  @Test
  public void getPropertyFromDefaultConfiguration() {
    Play.configuration.setProperty(
        "db.default.url", "jdbc:h2:mem:play;MODE=MSSQLServer;LOCK_MODE=0");
    Configuration configuration = new Configuration("default");
    assertThat(configuration.getProperty("db.url"))
        .isEqualTo("jdbc:h2:mem:play;MODE=MSSQLServer;LOCK_MODE=0");
  }
}
