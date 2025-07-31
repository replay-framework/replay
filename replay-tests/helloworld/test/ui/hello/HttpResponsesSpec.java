package ui.hello;

import static io.restassured.RestAssured.when;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.filter.log.LogDetail;
import io.restassured.response.Response;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

public class HttpResponsesSpec extends BaseSpec {

  @RepeatedTest(100)
  public void openStaticFile() {
    when()
        .get("/public/hello_world.txt")
        .then()
        .log()
        .ifValidationFails(LogDetail.ALL)
        .statusCode(200)
        .header("Content-Length", Integer::parseInt, equalTo(14))
        .header("Cache-Control", equalTo("max-age=3600"))
        .header("accept-ranges", equalTo("bytes"))
        .header("Last-Modified", notNullValue())
        .header("ETag", notNullValue())
        .contentType("text/plain; charset=UTF-8")
        .body(equalTo("Hello, WinRar!"));
  }

  @Test
  public void openMissingStaticFile() {
    when()
        .get("/public/goodbye_world.jpeg")
        .then()
        .log()
        .ifValidationFails(LogDetail.ALL)
        .statusCode(404)
        .header("Content-Length", Integer::parseInt, between(510, 530))
        .header("Cache-Control", nullValue())
        .header("accept-ranges", nullValue())
        .header("Last-Modified", nullValue())
        .header("ETag", nullValue())
        .contentType("text/html; charset=UTF-8")
        .body(containsString("The file app/public/goodbye_world.jpeg does not exist"));
  }

  @Test
  public void openImage() throws IOException {
    Response response = when().get("/img/favicon.png");
    response
        .then()
        .log()
        .ifValidationFails(LogDetail.ALL)
        .statusCode(200)
        .header("Content-Length", Integer::parseInt, equalTo(731))
        .header("Cache-Control", equalTo("max-age=3600"))
        .header("accept-ranges", equalTo("bytes"))
        .header("Last-Modified", notNullValue())
        .header("ETag", notNullValue())
        .contentType("image/png")
        .body(containsString("PNG"));
    assertThat(response.body().asByteArray())
        .isEqualTo(toByteArray(requireNonNull(getClass().getResourceAsStream("favicon.png"))));
  }

  @Test
  public void notFoundPage() throws IOException {
    Response response = when().get("/void");
    response.then().log().ifValidationFails(LogDetail.ALL).statusCode(404).contentType("text/html");
    assertThat(response.body().asString())
        .isEqualToIgnoringWhitespace(
            IOUtils.toString(
                requireNonNull(getClass().getResourceAsStream("not-found.html")), UTF_8));
    response.then().header("Content-Length", Integer::parseInt, between(460, 490));
  }

  @Test
  public void badRequest() {
    Response response = when().get("/repeat?times=zopa");
    response
        .then()
        .log()
        .ifValidationFails(LogDetail.ALL)
        .statusCode(400)
        .header("Content-Length", Integer::parseInt, equalTo(45))
        .contentType("text/html");
    assertThat(response.body().asString())
        .isEqualToIgnoringWhitespace("<h1>[validation.invalid, validation.min]</h1>");
  }

  @Test
  public void serverError() throws IOException {
    Response response = when().get("/epic-fail");
    response
        .then()
        .log()
        .ifValidationFails(LogDetail.ALL)
        .statusCode(500)
        .header("Content-Length", Integer::parseInt, between(260, 290))
        .contentType("text/html");
    assertThat(response.body().asString())
        .isEqualToIgnoringWhitespace(
            IOUtils.toString(
                requireNonNull(getClass().getResourceAsStream("server-error.html")), UTF_8));
  }

  public static <T extends Comparable<T>> Matcher<T> between(T lower, T upper) {
    return allOf(
        greaterThanOrEqualTo(lower),
        lessThanOrEqualTo(upper)
    );
  }
}
