package play.i18n;

import org.junit.Test;
import play.Play;
import play.PlayBuilder;
import play.mvc.Http;

import java.util.Arrays;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class LangTest {

    @Test
    public void testChange() {
        new PlayBuilder().build();
        Play.langs = Arrays.asList("no", "en", "fr");
        Lang.current.set(null);
        assertThat(Lang.current.get()).isNull();

        Lang.change("no");
        assertThat(Lang.current.get()).isEqualTo("no");
        Lang.change("nox");
        assertThat(Lang.current.get()).isEqualTo("no");
        Lang.change("EN");
        assertThat(Lang.current.get()).isEqualTo("en");
        Lang.change("fr");
        assertThat(Lang.current.get()).isEqualTo("fr");

        Lang.change("xx");
        assertThat(Lang.current.get()).isEqualTo("fr");

        Lang.change("en_uk");
        assertThat(Lang.current.get()).isEqualTo("en");

        Play.langs = Arrays.asList("no", "en", "en_uk", "fr");
        Lang.current.set(null);
        Lang.change("en_uk");
        assertThat(Lang.current.get()).isEqualTo("en_uk");
        Lang.change("en");
        assertThat(Lang.current.get()).isEqualTo("en");
        Lang.change("en_qw");
        assertThat(Lang.current.get()).isEqualTo("en");
    }

    @Test
    public void testGet() {
        new PlayBuilder().build();
        Play.langs = Arrays.asList("no", "en", "en_GB", "fr");
        Lang.current.set(null);

        Http.Response.setCurrent( new Http.Response());

        // check default when missing request
        Http.Request.setCurrent(null);
        assertThat(Lang.get()).isEqualTo("no");

        // check default when missing info in request
        Http.Request req = newRequest();
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("no"));

        // check only with accept-language,  without cookie value
        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("no"));

        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "no"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("no"));

        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "en"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x,en"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "en-GB"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en", "GB"));

        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x,en-GB"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en", "GB"));

        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "x,en-US"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));


        // prove lighthouse fix https://play.lighthouseapp.com/projects/57987/tickets/1302
        // space in accept language header
        req = newRequest();
        req.headers.put("accept-language", new Http.Header("accept-language", "nl, en;q=0.8"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));
        // check with cookie value

        req = newRequest();

        Http.Cookie cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "x";//not found in cookie
        req.cookies.put(cookie.name, cookie);
        req.headers.put("accept-language", new Http.Header("accept-language", "en"));
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "en";
        req.cookies.put(cookie.name, cookie);
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "en_q";
        req.cookies.put(cookie.name, cookie);
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en"));

        cookie = new Http.Cookie();
        cookie.name = "PLAY_LANG";
        cookie.value = "en_GB";
        req.cookies.put(cookie.name, cookie);
        Http.Request.setCurrent(req);
        Lang.current.set(null);
        assertLocale(new Locale("en", "GB"));


    }

    private void assertLocale(Locale locale) {
      assertThat(Lang.get()).isEqualTo(locale.toString());
      assertThat(Lang.getLocale()).isEqualTo(locale);
    }

    public static Http.Request newRequest() {
        return Http.Request.createRequest(null, "GET", "/", "", null, null, null, null, false, 80, "localhost", false, null, null);
    }
}
