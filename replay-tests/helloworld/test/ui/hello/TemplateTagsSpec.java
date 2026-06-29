package ui.hello;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

public class TemplateTagsSpec extends BaseSpec {

  @Test
  public void set_and_get_tag() {
    when().get("/tags").then().statusCode(200).body(containsString("Tags test"));
  }

  @Test
  public void ifnot_tag() {
    when().get("/tags").then().statusCode(200).body(containsString("visible"));
  }

  @Test
  public void elseif_tag() {
    when()
        .get("/tags")
        .then()
        .statusCode(200)
        .body(containsString("<p id=\"elseif\">two</p>"))
        .body(not(containsString("<p id=\"elseif\">one</p>")));
  }

  @Test
  public void render_tag() {
    when().get("/tags").then().statusCode(200).body(containsString("Rendered"));
  }

  @Test
  public void cache_tag() {
    when().get("/tags").then().statusCode(200).body(containsString("cached"));
  }

  @Test
  public void jsAction_tag() {
    when().get("/tags").then().statusCode(200).body(containsString("function(options)"));
  }

  @Test
  public void secureInlineJavaScript_tag() {
    when().get("/tags").then().statusCode(200).body(containsString("inline-js"));
  }

  @Test
  public void jsRoute_tag() {
    when().get("/tags").then().statusCode(200).body(containsString("url:"));
  }

  @Test
  public void a_tag() {
    when()
        .get("/tags")
        .then()
        .statusCode(200)
        .body(containsString("style=\"display:none\""))
        .body(containsString("post-link"));
  }

  @Test
  public void field_tag() {
    when()
        .get("/tag-form")
        .then()
        .statusCode(200)
        .body(containsString("<p id=\"field-name\">username</p>"))
        .body(containsString("<p id=\"field-error-class\">hasError</p>"));
  }

  @Test
  public void ifError_tag() {
    when().get("/tag-form").then().statusCode(200).body(containsString("has-error"));
  }

  @Test
  public void errorClass_tag() {
    when().get("/tag-form").then().statusCode(200).body(containsString("hasError"));
  }

  @Test
  public void error_tag() {
    when().get("/tag-form").then().statusCode(200).body(containsString("required"));
  }
}
