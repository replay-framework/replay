package play.mvc;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpResponseTest {

    @Test
    public void cookieDomain() {
        Http.Response response = new Http.Response();
        response.setCookie("testCookie", "testValue", "example.com", "/", 365, true, true);

        assertThat(response.cookies.get("testCookie").domain).isEqualTo("example.com");
        assertThat(response.cookies.get("testCookie").value).isEqualTo("testValue");
        assertThat(response.cookies.get("testCookie").maxAge).isEqualTo(365);
        assertThat(response.cookies.get("testCookie").path).isEqualTo("/");
        assertThat(response.cookies.get("testCookie").secure).isTrue();
        assertThat(response.cookies.get("testCookie").httpOnly).isTrue();
    }

    @Test
    public void cookieDomain_isOptional() {
        Http.Response response = new Http.Response();
        response.setCookie("testCookie", "testValue");
        assertThat(response.cookies.get("testCookie").domain).isNull();
    }
}
