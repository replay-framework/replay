package play.libs;

import org.junit.Test;
import play.utils.OS;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Marek Piechut
 */
public class FilesTest {

    @Test
    public void sanitizeFileName() {
        // File names to test are on odd indexes and expected results are on even indexes, ex:
        // test_file_name, expected_file_name
        String[] FILE_NAMES = { null, null, "", "", "a", "a", "test.file", "test.file", "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&",
                "validfilename-,^&'@{}[],$=!-#()%.+~_.&&&", "invalid/file", "invalid_file", "invalid\\file", "invalid_file",
                "invalid:*?\\<>|/file", "invalid________file", };

        for (int i = 0; i < FILE_NAMES.length; i += 2) {
            String actual = Files.sanitizeFileName(FILE_NAMES[i]);
            String expected = FILE_NAMES[i + 1];

            assertThat(actual)
              .as(() -> "String was not sanitized properly: " + actual)
              .isEqualTo(expected);
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
