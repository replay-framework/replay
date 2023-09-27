package play.mvc;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.Play;
import play.exceptions.NoRouteFoundException;
import play.mvc.Http.Request;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;

import java.util.Map;
import java.util.Properties;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RouterTest {

    @BeforeEach
    @AfterEach
    public void setUp() {
        Router.clearForTests();
    }

    @Test
    public void test_getBaseUrl() {

        Play.configuration = new Properties();

        // test with currentRequest
        Http.Request request = Http.Request.createRequest(
                null,
                "GET",
                "/",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost",
            null,
                null
        );

        Http.Request.setCurrent(request);
        assertThat(Router.getBaseUrl()).isEqualTo("http://localhost");

        // test without current request
        Http.Request.removeCurrent();
        // application.baseUrl without trailing /
        Play.configuration.setProperty("application.baseUrl", "http://a");
        assertThat(Router.getBaseUrl()).isEqualTo("http://a");

        // application.baseUrl with trailing /
        Play.configuration.setProperty("application.baseUrl", "http://b/");
        assertThat(Router.getBaseUrl()).isEqualTo("http://b");
    }

    @Test
    public void test_staticDir() {

        Play.configuration = new Properties();

        // we add a static route for a specific domain only
        Router.addRoute("GET", "/pics/", "staticDir:/public/images");
        // another static route with NO specific domain
        Router.addRoute("GET", "/music/", "staticDir:/public/mp3");

        // we request a static image file (which lives only on a specific domain)
        Http.Request imageRequest = Http.Request.createRequest(
                null,
                "GET",
                "/pics/chuck-norris.jpg",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost", // domain gets changed below a few times
            null,
                null
        );
        // we also request a static music file (which lives on NO specific domain)
        Http.Request musicRequest = Http.Request.createRequest(
                null,
                "GET",
                "/music/michael-jackson_black-or-white.mp3",
                "",
                null,
                null,
                null,
                null,
                false,
                80,
                "localhost", // domain gets changed below a few times
            null,
                null
        );

        // Test on localhost
        assertThat(canRenderFile(imageRequest)).isTrue();
        assertThat(canRenderFile(musicRequest)).isTrue();

        // Test on localhost:9000
        imageRequest.port = 9000;
        musicRequest.port = 9000;
        assertThat(canRenderFile(imageRequest)).isTrue();
        assertThat(canRenderFile(musicRequest)).isTrue();

        // we request the image file from a "wrong"/different domain, it will not be found
        imageRequest.port = 80;
        musicRequest.port = 80;
        imageRequest.domain = "google.com";
        assertThat(canRenderFile(imageRequest)).isTrue();

        // same for musicfile, but it will be rendered because the domain doesn't matter
        musicRequest.domain = "google.com";

        assertThat(canRenderFile(musicRequest))
          .as(() -> "Musicfile [" + musicRequest.domain + "] file  must be found")
          .isTrue();

        // we request the image file from the "right" domain
        imageRequest.domain = "example.com";
        assertThat(canRenderFile(imageRequest))
          .as(() -> "Image file [" + musicRequest.domain + "] from the right domain must be found")
          .isTrue();

        // same for musicfile, it will be rendered again also on this domain
        musicRequest.domain = "example.com";
        assertThat(canRenderFile(musicRequest))
          .as(() -> "Musicfile [" + musicRequest.domain + "] from the right domain must be found")
          .isTrue();
    }

    private boolean canRenderFile(Request request){
        try {
            Router.instance.route(request);
        } catch(RenderStatic rs) {
            return true;
        }  catch(NotFound nf) {
            return false;
        }
        return false;
    }

    @Test
    public void actionToUrl_withoutArguments() {
        assertThat(actionToUrl("com.blah.AuthController.doLogin", emptyMap())).isEqualTo("/auth/login");
    }

    @Test
    public void actionToUrl_withArguments() {
        assertThat(actionToUrl("cards.Requisites.showPopup", Map.of("cardId", "987654321")))
          .isEqualTo("/cards/987654321/requisites");
    }

    @Test
    public void actionToUrl_usesFirstArgumentFromList() {
        assertThat(actionToUrl("cards.Requisites.showPopup", Map.of("cardId", asList("987654321"))))
          .isEqualTo("/cards/987654321/requisites");
    }

    @Test
    public void actionToUrl_encodesArgumentsForUrl() {
        assertThat(actionToUrl("cards.Requisites.showPopup", Map.of("cardId", "/foo&bar'baz")))
          .isEqualTo("/cards/%2Ffoo%26bar%27baz/requisites");
    }

    @Test
    public void actionToUrl_withArgumentsInAction() {
        assertThat(actionToUrl("News.list", emptyMap())).isEqualTo("/news/list");
    }

    @Test
    public void actionToUrl_firstMatchingRouteWins() {
        assertThat(actionToUrl("News.index", emptyMap())).isEqualTo("/news");
    }

    @Test
    public void actionToUrl_skipsControllersPackage() {
        assertThat(actionToUrl("controllers.cards.Requisites.showPopup", Map.of("cardId", "987654321")))
          .isEqualTo("/cards/987654321/requisites");
    }

    @Test
    public void actionToUrl_withMissingArguments() {
        assertThatThrownBy(() -> actionToUrl("cards.Requisites.showPopup", emptyMap()))
          .isInstanceOf(NoRouteFoundException.class)
          .hasMessage("No route found for action cards.Requisites.showPopup with arguments {}");
    }

    @Test
    public void actionToUrl_catchAllRoute() {
        Router router = new Router(asList(
          new Router.Route("GET", "/{controller}/{action}", "{controller}.{action}", null, 0)
        ));

        assertThat(router.actionToUrl("MyController.myAction", emptyMap(), null, null).url).isEqualTo("/mycontroller/myaction");
        assertThat(router.actionToUrl("somePackage.MyController.myAction", emptyMap(), null, null).url).isEqualTo("/somepackage.mycontroller/myaction");
    }

    private Router router() {
        return new Router(asList(
          new Router.Route("GET", "/cards/{cardId}/requisites", "cards.Requisites.showPopup", null, 0),
          new Router.Route("GET", "/news", "News.index", null, 0),
          new Router.Route("GET", "/news/{method}", "News.{method}", null, 0),
          new Router.Route("POST", "/auth/login", "com.blah.AuthController.doLogin", null, 0),
          new Router.Route("GET", "/public/", "staticDir:public", null, 0),
          new Router.Route("GET", "/robots.txt", "staticFile:/public/robots.txt", null, 0)
        ));
    }

    private String actionToUrl(String action, Map<String, Object> args) {
        return router().actionToUrl(action, args, null, null).toString();
    }
}
