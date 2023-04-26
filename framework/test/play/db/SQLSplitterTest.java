package play.db;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

public class SQLSplitterTest {

    @Test
    public void verifyConsumeLine() {
      assertThat(SQLSplitter.consumeTillNextLine("abc\ra", 0)).isEqualTo(4);
    }

    @Test
    public void verifySkipComments() {
      assertThat(SQLSplitter.consumeComment("--hello\rSELECT * from STUDENTS;", 0)).isEqualTo(8);
      assertThat(SQLSplitter.consumeComment("--hello\nSELECT * from STUDENTS;", 0)).isEqualTo(8);

      assertThat(SQLSplitter.consumeComment("#hello\r\nSELECT * from STUDENTS;", 0)).isEqualTo(8);
      assertThat(SQLSplitter.consumeComment("#hello\rSELECT * from STUDENTS;", 0)).isEqualTo(7);
      assertThat(SQLSplitter.consumeComment("#hello\nSELECT * from STUDENTS;", 0)).isEqualTo(7);

      assertThat(SQLSplitter.consumeComment("/*h\r\nw*/SELECT * from STUDENTS;", 0)).isEqualTo(8);
      assertThat(SQLSplitter.consumeComment("/*hello*/SELECT * from STUDENTS;", 0)).isEqualTo(9);
    }

    @Test
    public void verifyDontSkipComments() {
      assertThat(SQLSplitter.consumeComment("SELECT * from STUDENTS;", 0)).isEqualTo(0);
      assertThat(SQLSplitter.consumeComment("SELECT * from STUDENTS; #h", 0)).isEqualTo(0);
      assertThat(SQLSplitter.consumeComment("-a * from STUDENTS;", 0)).isEqualTo(0);
      assertThat(SQLSplitter.consumeComment("a * from STUDENTS; #h", 0)).isEqualTo(0);

      assertThat(SQLSplitter.consumeComment("/*hello*/SELECT * from STUDENTS;", 9)).isEqualTo(9);
      assertThat(SQLSplitter.consumeComment("#hello\nSELECT * from STUDENTS;", 7)).isEqualTo(7);
    }

    @Test
    public void verifySkipStandardQuotes() {
      assertThat(SQLSplitter.consumeQuote("\"12\"w", 0)).isEqualTo(4);
      assertThat(SQLSplitter.consumeQuote("'12'w", 0)).isEqualTo(4);
      assertThat(SQLSplitter.consumeQuote("`12`w", 0)).isEqualTo(4);
      assertThat(SQLSplitter.consumeQuote("[12]w", 0)).isEqualTo(4);
    }

    @Test
    public void verifyDontSkipStandardQuotes() {
      assertThat(SQLSplitter.consumeQuote("\"12\"w", 4)).isEqualTo(4);
      assertThat(SQLSplitter.consumeQuote("'12'w", 4)).isEqualTo(4);
      assertThat(SQLSplitter.consumeQuote("`12`w", 4)).isEqualTo(4);
      assertThat(SQLSplitter.consumeQuote("[12]w", 4)).isEqualTo(4);

      assertThat(SQLSplitter.consumeQuote("123", 0)).isEqualTo(0);
    }

    @Test
    public void verifySkipDollarQuotes() {
      assertThat(SQLSplitter.consumeQuote("$$a$$b", 0)).isEqualTo(5);
      assertThat(SQLSplitter.consumeQuote("$$ab$$c", 0)).isEqualTo(6);
      assertThat(SQLSplitter.consumeQuote("$1$a$1$b", 0)).isEqualTo(7);
      assertThat(SQLSplitter.consumeQuote("$1$a\n$1$b", 0)).isEqualTo(8);
      assertThat(SQLSplitter.consumeQuote("$12$a$12$b", 0)).isEqualTo(9);
      assertThat(SQLSplitter.consumeQuote("$12$ab$12$c", 0)).isEqualTo(10);
      assertThat(SQLSplitter.consumeQuote("$12$a\nb$12$c", 0)).isEqualTo(11);

      assertThat(SQLSplitter.consumeQuote("$1$ $f$ $f$ $1$a", 0)).isEqualTo(15);
      assertThat(SQLSplitter.consumeQuote("$1$$f$$f$$1$a", 0)).isEqualTo(12);
      assertThat(SQLSplitter.consumeQuote("$1$$f$\n$f$$1$a", 0)).isEqualTo(13);
    }


    @Test
    public void verifySkipParentheses() {
      assertThat(SQLSplitter.consumeParentheses("(())", 0)).isEqualTo(4);
      assertThat(SQLSplitter.consumeParentheses("(())a", 0)).isEqualTo(4);
      assertThat(SQLSplitter.consumeParentheses("((b))a", 0)).isEqualTo(5);
      assertThat(SQLSplitter.consumeParentheses("(c(b)c)a", 0)).isEqualTo(7);
      assertThat(SQLSplitter.consumeParentheses("(c(\n)c)a", 0)).isEqualTo(7);
      assertThat(SQLSplitter.consumeParentheses("((')'))a", 0)).isEqualTo(7);
      assertThat(SQLSplitter.consumeParentheses("((/*)*/))a", 0)).isEqualTo(9);
      assertThat(SQLSplitter.consumeParentheses("(name varchar);", 0)).isEqualTo(14);
    }

    @Test
    public void verifyTrailingParenthesis() {
      assertThat(SQLSplitter.consumeParentheses("(", 0)).isEqualTo(1);
      assertThat(SQLSplitter.consumeParentheses("(()", 0)).isEqualTo(3);
    }

    @Test
    public void verifyDoubleSemicolonHandling() {
      assertThat(SQLSplitter.splitSQL("a;\nb;;\nc;").size()).isEqualTo(2);
      assertThat(SQLSplitter.splitSQL("a;\nb;\nc;").size()).isEqualTo(3);
    }

    String readFile(String filename) {
        try {
            return IOUtils.toString(requireNonNull(getClass().getResource(filename), 
              () -> String.format("Test resource %s not found", filename)), UTF_8);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void verifyTestSplitting() {
        List<CharSequence> srcArrList = SQLSplitter.splitSQL(readFile("/play/db/test.sql"));
        CharSequence[] srcArr = new CharSequence[(int) srcArrList.size()];
        srcArr = srcArrList.toArray(srcArr);

      assertThat(srcArr).isEqualTo(readFile("/play/db/test.out.sql").split("==="));

        srcArrList = SQLSplitter.splitSQL(readFile("/play/db/test2.sql"));
        srcArr = new CharSequence[(int) srcArrList.size()];
        srcArr = srcArrList.toArray(srcArr);
      assertThat(srcArr).isEqualTo(readFile("/play/db/test2.out.sql").split("==="));
    }
}
