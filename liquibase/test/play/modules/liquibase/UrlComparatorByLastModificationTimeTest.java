package play.modules.liquibase;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class UrlComparatorByLastModificationTimeTest {
  UrlComparatorByLastModificationTime comparator = new UrlComparatorByLastModificationTime();
  String oldFile, newFile;

  @Before
  public void setUp() throws IOException, URISyntaxException {
    oldFile = new File("src/play/modules/liquibase/DuplicatesIgnoringResourceAccessor.java").toURI().toURL().toExternalForm();
    newFile = getClass().getResource("DuplicatesIgnoringResourceAccessor.class").toString();
  }

  @Test
  public void newestFilesFirst() throws IOException {
    List<String> files = new ArrayList<>(asList(oldFile, newFile));
    Collections.sort(files, comparator);
    
    assertEquals(newFile, files.get(0));
    assertEquals(oldFile, files.get(1));
  }
  
  @Test
  public void compareFilesByLastModificationTime() throws IOException {
    assertEquals(1, comparator.compare(oldFile, newFile));
    assertEquals(-1, comparator.compare(newFile, oldFile));
    assertEquals(0, comparator.compare(oldFile, oldFile));
    assertEquals(0, comparator.compare(newFile, newFile));
  }
}