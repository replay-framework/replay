package play.libs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.io.File;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import play.utils.OS;

/** @author Marek Piechut */
public class FilesTest {

  @ParameterizedTest
  @MethodSource("fileNames")
  public void sanitizeFileName(FileName fileName) {
    assertThat(Files.sanitizeFileName(fileName.raw)).isEqualTo(fileName.sanitized);
  }

  private static Stream<FileName> fileNames() {
    return Stream.of(
        new FileName(null, null),
        new FileName("", ""),
        new FileName("a", "a"),
        new FileName("test.file", "test.file"),
        new FileName(
            "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&", "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&"),
        new FileName("invalid/file", "invalid_file"),
        new FileName("invalid\\file", "invalid_file"),
        new FileName("invalid:*?\\<>|/file", "invalid________file"));
  }

  private static class FileName {
    private final String raw;
    private final String sanitized;

    private FileName(String raw, String sanitized) {
      this.raw = raw;
      this.sanitized = sanitized;
    }
  }

  @Test
  public void fileEqualsOnWindows_sameName() {
    assumeThat(OS.isWindows()).isTrue();

    var a = new File("C:\\temp\\TEST.TXT");
    var b = new File("C:\\temp\\TEST.TXT");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEqualsOnWindows_differentCase() {
    assumeThat(OS.isWindows()).isTrue();

    var a = new File("C:\\temp\\TEST.TXT");
    var b = new File("C:\\temp\\test.txt");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEqualsOnWindows_pathWithDot() {
    assumeThat(OS.isWindows()).isTrue();

    var a = new File("C:\\temp\\TEST.TXT");
    var b = new File("C:\\temp\\.\\test.txt");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEqualsOnWindows_pathWithTwoDots() {
    assumeThat(OS.isWindows()).isTrue();
    var a = new File("C:\\temp\\..\\TEMP\\TEST.TXT");
    var b = new File("C:\\temp\\.\\test.txt");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEquals_sameCase() {
    var a = new File("temp\\TEST.TXT");
    var b = new File("temp\\TEST.TXT");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEquals_sameCase_fromRoot() {
    var a = new File("\\temp\\TEST.TXT");
    var b = new File("\\temp\\TEST.TXT");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEquals_differentCase() {
    var a = new File("\\temp\\TEST.TXT");
    var b = new File("\\temp\\test.txt");

    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isEqualTo(OS.isWindows());
  }

  @Test
  public void fileEquals_sameCase_linuxStyle() {
    var a = new File("/temp/TEST.TXT");
    var b = new File("/temp/TEST.TXT");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEquals_differentCase_linuxStyle() {
    var a = new File("/temp/TEST.TXT");
    var b = new File("/temp/test.txt");

    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isEqualTo(OS.isWindows());
  }

  @Test
  public void fileEqualsWithParentCurrentFolder() {
    var a = new File("\\temp\\test.txt");
    var b = new File("\\temp\\.\\test.txt");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isEqualTo(OS.isWindows());
  }

  @Test
  public void fileEqualsWithParentCurrentFolder2() {
    var a = new File("/temp/../temp/test.txt");
    var b = new File("/temp/test.txt");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEqualsWithParentCurrentFolder3() {
    var a = new File("/temp/test.txt");
    var b = new File("/temp/./test.txt");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }

  @Test
  public void fileEqualsWithParentCurrentFolder4() {
    var a = new File("/temp/../temp/test.txt");
    var b = new File("/temp/./test.txt");
    assertThat(Files.isSameFile(a, b))
        .as(() -> String.format("Error comparing %s and %s", a.getPath(), b.getPath()))
        .isTrue();
  }
}
