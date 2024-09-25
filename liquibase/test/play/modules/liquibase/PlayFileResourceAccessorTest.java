package play.modules.liquibase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
}
