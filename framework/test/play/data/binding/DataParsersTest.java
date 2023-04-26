package play.data.binding;

import org.junit.jupiter.api.Test;
import play.data.parsing.ApacheMultipartParser;
import play.data.parsing.DataParsers;
import play.data.parsing.TextParser;
import play.data.parsing.UrlEncodedParser;

import static org.assertj.core.api.Assertions.assertThat;

public class DataParsersTest {
    @Test
    public void getDataParserDependingOnContentType() {
      assertThat(DataParsers.forContentType("application/x-www-form-urlencoded").getClass()).isEqualTo(UrlEncodedParser.class);
      assertThat(DataParsers.forContentType("multipart/form-data").getClass()).isEqualTo(ApacheMultipartParser.class);
      assertThat(DataParsers.forContentType("multipart/mixed").getClass()).isEqualTo(ApacheMultipartParser.class);
      assertThat(DataParsers.forContentType("application/xml").getClass()).isEqualTo(TextParser.class);
      assertThat(DataParsers.forContentType("application/json").getClass()).isEqualTo(TextParser.class);
    }

    @Test
    public void usesTextDataProviderForAnyContentTypeStartingWithText() {
      assertThat(DataParsers.forContentType("text/").getClass()).isEqualTo(TextParser.class);
      assertThat(DataParsers.forContentType("text/plain").getClass()).isEqualTo(TextParser.class);
      assertThat(DataParsers.forContentType("text/anything else").getClass()).isEqualTo(TextParser.class);
    }

    @Test
    public void returnsNullForUnsupportedContentTypes() {
      assertThat(DataParsers.forContentType("unknown")).isNull();
      assertThat(DataParsers.forContentType("")).isNull();
      assertThat(DataParsers.forContentType("text")).isNull();
    }
}
