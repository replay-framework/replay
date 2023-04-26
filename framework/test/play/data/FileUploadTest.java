package play.data;

import org.apache.commons.fileupload.FileItem;
import org.junit.Before;
import org.junit.Test;
import play.Play;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileUploadTest {
  @Before
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
