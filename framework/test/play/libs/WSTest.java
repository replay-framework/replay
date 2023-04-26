package play.libs;

import org.junit.jupiter.api.Test;
import play.libs.ws.DummyHttpResponse;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WS} class.
 */
public class WSTest {
    
    @Test
    public void getQueryStringTest(){
        DummyHttpResponse response = new DummyHttpResponse(200, "a=&b= etc");
        Map<String, String>  queryStr = response.getQueryString();
      assertThat(queryStr).isNotNull();
      assertThat(queryStr.get("a")).isEqualTo("");
      assertThat(queryStr.get("b")).isEqualTo(" etc");
      assertThat(queryStr.size()).isEqualTo(2);
    }
    
    @Test
    public void getQueryStringTest1(){
        DummyHttpResponse response = new DummyHttpResponse(200, "a&b= etc&&&d=test toto");
        Map<String, String>  queryStr = response.getQueryString();
      assertThat(queryStr).isNotNull();
      assertThat(queryStr.get("a")).isEqualTo("");
      assertThat(queryStr.get("b")).isEqualTo(" etc");
      assertThat(queryStr.get("")).isEqualTo("");
      assertThat(queryStr.get("d")).isEqualTo("test toto");
      assertThat(queryStr.size()).isEqualTo(4);
    }
    
    @Test
    public void getQueryStringTest2(){
        DummyHttpResponse response = new DummyHttpResponse(200, "&a&b= etc&&d=**");
        Map<String, String>  queryStr = response.getQueryString();
      assertThat(queryStr).isNotNull();
      assertThat(queryStr.get("a")).isEqualTo("");
      assertThat(queryStr.get("b")).isEqualTo(" etc");
      assertThat(queryStr.get("")).isEqualTo("");
      assertThat(queryStr.get("d")).isEqualTo("**");
      assertThat(queryStr.size()).isEqualTo(4);
    }
}
