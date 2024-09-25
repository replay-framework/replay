package play.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import org.apache.commons.fileupload.FileItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.Play;

public class FileUploadTest {
  @BeforeEach
  public void setUp() {
    Play.tmpDir = new File("tmp");
  }

  @Test
  public void sizeIsNullForMissingFile() {
    FileItem fileItem = mock(FileItem.class);
    when(fileItem.getName()).thenReturn("some-missing-file.pdf");

    assertThat(new FileUpload(fileItem).getSize()).isEqualTo(0);
  }
}
