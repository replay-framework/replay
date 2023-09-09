package ui.hello;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.filter.log.LogDetail;
import io.restassured.response.Response;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

/**
 * Similar to the error response tests in HttpResponsesSpec but with the TextErrorHandler
 */
public class HttpErrorResponsesSpec extends SimpleErrorHandlerBaseSpec {

  @Test
  public void openMissingStaticFilePlain() {
    given()
        .header("Accept", "text/plain")
        .when()
        .get("/public/goodbye_world.jpeg")
        .then()
        .log().ifValidationFails(LogDetail.ALL)
        .statusCode(404)
        .header("Content-Length", Integer::parseInt, equalTo(64))
        .header("Cache-Control", nullValue())
        .header("accept-ranges", nullValue())
        .header("Last-Modified", nullValue())
        .header("ETag", nullValue())
        .contentType("text/plain; charset=UTF-8")
        .body(containsString("The file app/public/goodbye_world.jpeg does not exist"));
  }

  @Test
  public void openMissingStaticFileHtml() {
    given()
        .header("Accept", "text/html")
        .when()
        .get("/public/goodbye_world.jpeg")
        .then()
        .log().ifValidationFails(LogDetail.ALL)
        .statusCode(404)
        .header("Content-Length", Integer::parseInt, equalTo(247))
        .header("Cache-Control", nullValue())
        .header("accept-ranges", nullValue())
        .header("Last-Modified", nullValue())
        .header("ETag", nullValue())
        .contentType("text/html; charset=UTF-8")
        .body(containsString("The file app/public/goodbye_world.jpeg does not exist"));
  }

  @Test
  public void notFoundPlain() throws IOException {
    Response response = given()
        .header("Accept", "text/plain")
        .when()
        .get("/void");
    response.then()
        .log().ifValidationFails(LogDetail.ALL)
        .statusCode(404)
        .contentType("text/plain");
    response.then()
        .header("Content-Length", Integer::parseInt, equalTo(20));
  }

  @Test
  public void notFoundHtml() throws IOException {
    Response response = when()
        .get("/void");
    response.then()
        .log().ifValidationFails(LogDetail.ALL)
        .statusCode(404)
        .contentType("text/html");
    assertThat(response.body().asString()).isEqualToIgnoringWhitespace(
        IOUtils.toString(requireNonNull(getClass().getResourceAsStream("not-found-simple.html")), UTF_8));
    response.then()
        .header("Content-Length", Integer::parseInt, equalTo(203));
  }

  @Test
  public void badRequest() {
    Response response = when().get("/repeat?times=zopa");
    response.then().log().ifValidationFails(LogDetail.ALL).statusCode(400)
        .header("Content-Length", Integer::parseInt, equalTo(232))
        .contentType("text/html");
    assertThat(response.body().asString()).contains("<p>[validation.invalid, validation.min]</p>");
  }

  @Test
  public void serverError() throws IOException {
    Response response = when()
        .get("/epic-fail");
    response.then()
        .log().ifValidationFails(LogDetail.ALL)
        .statusCode(500)
        .header("Content-Length", Integer::parseInt, equalTo(242))
        .contentType("text/html");
    assertThat(response.body().asString()).isEqualToIgnoringWhitespace(
        IOUtils.toString(requireNonNull(getClass().getResourceAsStream("server-error-simple.html")),
            UTF_8));
  }

  @Test
  public void notFound() {
    Response response = when()
        .get("/non-existing-path");
    response.then()
        .log().ifValidationFails(LogDetail.ALL)
        .statusCode(404)
        .header("Content-Length", Integer::parseInt, equalTo(216)).contentType("text/html");
  }
}
