package play;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.Test;

class ClasspathResourceTest {
  @Test
  void extractsNameOfJarFile_from_classpathURL() throws MalformedURLException {
    assertThat(
            new ClasspathResource(
                    "routes.yml",
                    new URL("jar:file:/Users/toomas/replay/build/libs/app-1.2.3.jar!/routes.yml"))
                .getJarFilePath())
        .isEqualTo("/Users/toomas/replay/build/libs/app-1.2.3.jar");
  }
}
