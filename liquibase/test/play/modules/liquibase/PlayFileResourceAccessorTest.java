package play.modules.liquibase;

import liquibase.resource.InputStreamList;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PlayFileResourceAccessorTest {
  private final PlayFileResourceAccessor accessor = new PlayFileResourceAccessor();

  @Test
  public void path_without_base() {
    assertThat(accessor.getPath(null, "db/auth.xml")).isEqualTo("db/auth.xml");
  }

  @Test
  public void path_with_base_file() {
    assertThat(accessor.getPath("db/auth.xml", "users_view.sql")).isEqualTo("db/users_view.sql");
  }

  @Test
  public void resources_from_classpath() {
    InputStreamList streamList = accessor.openStreams(null, "www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.2.xsd");
    assertThat(streamList.size()).isEqualTo(1);
    assertThat(streamList.getURIs().get(0).toString())
        .matches("jar:.+liquibase-core-.+\\.jar!/www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.2.xsd");
  }
}