package play.templates;

import org.codehaus.groovy.runtime.NullObject;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaExtensionsTest {

    @BeforeClass
    public static void setUpBeforeClass() {
    }

    @AfterClass
    public static void tearDownAfterClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testContains()  {
        String[] testArray = {"a", "b", "c"};
        assertThat(JavaExtensions.contains(testArray, "a")).isTrue();
        assertThat(JavaExtensions.contains(testArray, "1")).isFalse();
    }

    @Test
    public void testAdd()  {
        String[] testArray = {"a", "b", "c"};
        assertThat(JavaExtensions.add(new String[]{"a", "b"}, "c")).hasSize(3).contains(testArray);
        
    }

    @Test 
    public void testRemove()  {
        String[] testArray = {"a", "b", "c"};
        assertThat(JavaExtensions.remove(testArray, "c")).hasSize(2).contains("a", "b");
    }

    @Test
    public void testCapitalizeWords()  {
        assertThat(JavaExtensions.capitalizeWords("This is a small   test!")).isEqualTo("This Is A Small   Test!");
    }

    @Test 
    public void testPad()  {
        assertThat(JavaExtensions.pad("12345", 4)).isEqualTo("12345");
        assertThat(JavaExtensions.pad("12345", 5)).isEqualTo("12345");
        assertThat(JavaExtensions.pad("12345", 6)).isEqualTo("12345&nbsp;");
        assertThat(JavaExtensions.pad("12345", 8)).isEqualTo("12345&nbsp;&nbsp;&nbsp;");
    }

    @Test
    public void testEscapeJavaScript() {
        assertThat(JavaExtensions.escapeJavaScript("'Hello/world'")).isEqualTo("\\'Hello\\/world\\'");
        assertThat(JavaExtensions.escapeJavaScript("\u0001Привет\t你好\n")).isEqualTo("\\u0001Привет\\t你好\\n");
    }

    @Test
    public void testPluralizeNumber() {
      assertThat(JavaExtensions.pluralize(0)).isEqualTo("s");
      assertThat(JavaExtensions.pluralize(1)).isEqualTo("");
      assertThat(JavaExtensions.pluralize(2)).isEqualTo("s");
    }

    @Test
    public void testPluralizeCollection() {
        List <String> testCollection = new ArrayList<>();
      assertThat(JavaExtensions.pluralize(testCollection)).isEqualTo("s");
        testCollection.add("1");
      assertThat(JavaExtensions.pluralize(testCollection)).isEqualTo("");
        testCollection.add("2");
      assertThat(JavaExtensions.pluralize(testCollection)).isEqualTo("s");
    }

    @Test
    public void testPluralizeNumberString() {
        String plural = "n";
      assertThat(JavaExtensions.pluralize(0, plural)).isEqualTo(plural);
      assertThat(JavaExtensions.pluralize(1, plural)).isEqualTo("");
      assertThat(JavaExtensions.pluralize(2, plural)).isEqualTo(plural);
    }

    @Test
    public void testPluralizeCollectionString() {
        String plural = "n";
        List <String> testCollection = new ArrayList<>();
      assertThat(JavaExtensions.pluralize(testCollection, plural)).isEqualTo(plural);
        testCollection.add("1");
      assertThat(JavaExtensions.pluralize(testCollection, plural)).isEqualTo("");
        testCollection.add("2");
      assertThat(JavaExtensions.pluralize(testCollection, plural)).isEqualTo(plural);
    }

    @Test
    public void testPluralizeNumberStringArray() {
        String[] forms = {"Test", "Tests"};
      assertThat(JavaExtensions.pluralize(0, forms)).isEqualTo(forms[1]);
      assertThat(JavaExtensions.pluralize(1, forms)).isEqualTo(forms[0]);
      assertThat(JavaExtensions.pluralize(2, forms)).isEqualTo(forms[1]);

    }

    @Test
    public void testPluralizeCollectionStringArray() {
        String[] forms = {"Test", "Tests"};
        List <String> testCollection = new ArrayList<>();
      assertThat(JavaExtensions.pluralize(testCollection, forms)).isEqualTo(forms[1]);
        testCollection.add("1");
      assertThat(JavaExtensions.pluralize(testCollection, forms)).isEqualTo(forms[0]);
        testCollection.add("2");
      assertThat(JavaExtensions.pluralize(testCollection, forms)).isEqualTo(forms[1]);
    }

    @Test
    public void testYesNo()  {
        String yes = "Y";
        String no = "N";
        String[] yesNo = {yes, no};
      assertThat(JavaExtensions.yesno(null, yesNo)).isEqualTo(no);
      assertThat(JavaExtensions.yesno(Boolean.FALSE, yesNo)).isEqualTo(no);
      assertThat(JavaExtensions.yesno(Boolean.TRUE, yesNo)).isEqualTo(yes);
        //String
      assertThat(JavaExtensions.yesno("", yesNo)).isEqualTo(no);
      assertThat(JavaExtensions.yesno("Test", yesNo)).isEqualTo(yes);
        //Number
      assertThat(JavaExtensions.yesno(0L, yesNo)).isEqualTo(no);
      assertThat(JavaExtensions.yesno(1L, yesNo)).isEqualTo(yes);
      assertThat(JavaExtensions.yesno(-1L, yesNo)).isEqualTo(yes);
        //Collection
        List <String> testCollection = new ArrayList<>();
      assertThat(JavaExtensions.yesno(testCollection, yesNo)).isEqualTo(no);
        testCollection.add("1");
      assertThat(JavaExtensions.yesno(testCollection, yesNo)).isEqualTo(yes);
        // NullObject
        NullObject nullObject = NullObject.getNullObject();
      assertThat(JavaExtensions.yesno(nullObject, yesNo)).isEqualTo(no);
    }

    @Test 
    public void testLast()  {
        List <String> testCollection = new ArrayList<>();
        testCollection.add("1");
        testCollection.add("2");
      assertThat(JavaExtensions.last(testCollection)).isEqualTo("2");
    }

    @Test 
    public void testJoin()  {
        List <String> testCollection = new ArrayList<>();
        testCollection.add("1");
        testCollection.add("2");

      assertThat(JavaExtensions.join(testCollection, ", ")).isEqualTo("1, 2");
    }

}
