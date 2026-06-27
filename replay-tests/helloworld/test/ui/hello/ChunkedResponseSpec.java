package ui.hello;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class ChunkedResponseSpec extends BaseSpec {

  @Test
  public void chunkedResponse() {
    when()
        .get("/chunked")
        .then()
        .log()
        .ifValidationFails()
        .statusCode(200)
        .header("Transfer-Encoding", equalTo("chunked"))
        .header("Content-Length", nullValue())
        .body(equalTo("first chunk\nsecond chunk\nthird chunk\n"));
  }
}
