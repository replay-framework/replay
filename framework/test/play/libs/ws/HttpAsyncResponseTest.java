package play.libs.ws;

import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.Response;
import org.junit.jupiter.api.Test;
import play.mvc.Http;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpAsyncResponseTest {
  @Test
  public void headers() {
    Response r = mock(Response.class);
    FluentCaseInsensitiveStringsMap headers = new FluentCaseInsensitiveStringsMap();
    headers.put("content-length", asList("32"));
    headers.put("content-type", asList("text/html"));
    headers.put("x-forwarded-for", asList("1.1.1", "2.2.2", "3.3.3"));
    when(r.getHeaders()).thenReturn(headers);

    HttpAsyncResponse response = new HttpAsyncResponse(r);

    assertThat(response.getHeaders()).usingFieldByFieldElementComparator().isEqualTo(asList(
      new Http.Header("content-length", "32"),
      new Http.Header("content-type", "text/html"),
      new Http.Header("x-forwarded-for", asList("1.1.1", "2.2.2", "3.3.3"))
    ));
  }
}