package play.modules.liquibase;

import org.junit.Before;
import org.junit.Test;
import play.Play;
import play.mvc.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LiquibasePluginTest {
  LiquibasePlugin plugin = new LiquibasePlugin();

  @Before
  @After
  public void setUp() {
    Play.configuration.clear();
  }

  @Test
  public void parsesContexts() {
    Play.configuration.setProperty("liquibase.contexts", "dev,test");
    assertEquals("dev,test", plugin.parseContexts());
  }

  @Test
  public void contextsIsNullIfNotGiven() {
    Play.configuration.setProperty("liquibase.contexts", "");
    assertNull(plugin.parseContexts());
  }
}
