package play.libs;

import org.junit.Test;
import play.libs.ws.DummyHttpResponse;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Tests for {@link WS} class.
 */
public class WSTest {
    
    @Test
    public void getQueryStringTest(){
        DummyHttpResponse response = new DummyHttpResponse(200, "a=&b= etc");
        Map<String, String>  queryStr = response.getQueryString();
        assertNotNull(queryStr);
        assertEquals("", queryStr.get("a"));
        assertEquals(" etc", queryStr.get("b")); 
        assertEquals(2, queryStr.size());
    }
    
    @Test
    public void getQueryStringTest1(){
        DummyHttpResponse response = new DummyHttpResponse(200, "a&b= etc&&&d=test toto");
        Map<String, String>  queryStr = response.getQueryString();
        assertNotNull(queryStr);
        assertEquals("", queryStr.get("a"));
        assertEquals(" etc", queryStr.get("b"));
        assertEquals("", queryStr.get(""));
        assertEquals("test toto", queryStr.get("d"));     
        assertEquals(4, queryStr.size());
    }
    
    @Test
    public void getQueryStringTest2(){
        DummyHttpResponse response = new DummyHttpResponse(200, "&a&b= etc&&d=**");
        Map<String, String>  queryStr = response.getQueryString();
        assertNotNull(queryStr);
        assertEquals("", queryStr.get("a"));
        assertEquals(" etc", queryStr.get("b"));
        assertEquals("", queryStr.get(""));
        assertEquals("**", queryStr.get("d")); 
        assertEquals(4, queryStr.size());
    }
}
