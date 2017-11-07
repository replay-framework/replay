package play.modules.liquibase;

import org.junit.Before;
import org.junit.Test;
import play.Play;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static play.modules.liquibase.LiquibaseAction.UPDATE;
import static play.modules.liquibase.LiquibaseAction.VALIDATE;

public class LiquibasePluginTest {
  LiquibasePlugin plugin = new LiquibasePlugin();

  @Before
  public void setUp() {
    Play.configuration.clear();
  }

  @Test
  public void parsesActions() {
    Play.configuration.setProperty("liquibase.actions", "validate,update");
    assertEquals(asList(VALIDATE, UPDATE), new LiquibasePlugin().parseLiquibaseActions());
  }

  @Test(expected = LiquibaseUpdateException.class)
  public void atLeastOneActionMustBeConfigured() {
    Play.configuration.setProperty("liquibase.actions", "");
    plugin.parseLiquibaseActions();
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
