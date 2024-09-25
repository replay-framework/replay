package play.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import play.PlayBuilder;
import play.i18n.Messages;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Scope.Flash;
import play.mvc.Scope.Params;
import play.mvc.Scope.Session;

public class ScopeTest {

  private final Request request = new Request();

  @org.junit.jupiter.api.BeforeEach
  public void playBuilderBefore() {
    new PlayBuilder().build();
  }

  private static void mockRequestAndResponse() {
    Request.removeCurrent();
    Response.removeCurrent();
  }

  @Test
  public void testParamsPut() {
    mockRequestAndResponse();
    Params params = new Params(request);
    params.put("param1", "test");
    params.put("param1.test", "test2");

    params.put("param1.object", "obj");
    params.put("param1.object.param1", "param1");
    params.put("param1.object.param2", "param2");
    params.put("param1.object.param2.3", "param3");

    assertThat(params.all().size()).isEqualTo(6);

    assertThat(params.contains("param1")).isTrue();
    assertThat(params.contains("param1.object")).isTrue();
    assertThat(params.contains("param1.test")).isTrue();
    assertThat(params.contains("param1.object.param1")).isTrue();
    assertThat(params.contains("param1.object.param2")).isTrue();
    assertThat(params.contains("param1.object.param2.3")).isTrue();
  }

  @Test
  public void testParamsRemove() {
    mockRequestAndResponse();
    Params params = new Params(request);
    params.put("param1", "test");
    params.put("param1.test", "test2");

    params.put("param1.object", "obj");
    params.put("param1.object.param1", "param1");
    params.put("param1.object.param2", "param2");
    params.put("param1.object.param2.3", "param3");

    assertThat(params.all().size()).isEqualTo(6);

    params.remove("param1.object.param2");

    assertThat(params.contains("param1")).isTrue();
    assertThat(params.contains("param1.test")).isTrue();
    assertThat(params.contains("param1.object")).isTrue();
    assertThat(params.contains("param1.object.param1")).isTrue();
    assertThat(params.contains("param1.object.param2")).isFalse();
    assertThat(params.contains("param1.object.param2.3")).isTrue();

    assertThat(params.all().size()).isEqualTo(5);
  }

  @Test
  public void testParamsRemove2() {
    mockRequestAndResponse();
    Params params = new Params(request);
    params.put("param1", "test");
    params.put("param1.test", "test2");

    params.put("param1.object", "obj");
    params.put("param1.object.param1", "param1");
    params.put("param1.object.param2", "param2");
    params.put("param1.object.param2.3", "param3");

    assertThat(params.all().size()).isEqualTo(6);

    params.remove("param1.object");

    assertThat(params.contains("param1")).isTrue();
    assertThat(params.contains("param1.test")).isTrue();
    assertThat(params.contains("param1.object")).isFalse();
    assertThat(params.contains("param1.object.param1")).isTrue();
    assertThat(params.contains("param1.object.param2")).isTrue();
    assertThat(params.contains("param1.object.param2.3")).isTrue();

    assertThat(params.all().size()).isEqualTo(5);
  }

  @Test
  public void sessionPutWithNullObject() {
    Session session = new Session();
    session.put("hello", (Object) null);
    assertThat(session.get("hello")).isNull();
  }

  @Test
  public void sessionPutWithObject() {
    Session session = new Session();
    session.put("hello", 123);
    assertThat(session.get("hello")).isEqualTo("123");
  }

  @Test
  public void sessionPutWithNullString() {
    Session session = new Session();
    session.put("hello", null);
    assertThat(session.get("hello")).isNull();
  }

  @Test
  public void sessionPutWithString() {
    Session session = new Session();
    session.put("hello", "world");
    assertThat(session.get("hello")).isEqualTo("world");
  }

  @Test
  public void sessionWithOnlyId_isEmpty() {
    Session session = new Session();
    assertThat(session.getId()).isNotEmpty();
    assertThat(session.isEmpty()).isTrue();
  }

  @Test
  public void flashErrorFormat() {
    Flash flash = new Flash();
    flash.error("Your name is %s", "Hello");

    assertThat(flash.out.get("error")).isEqualTo("Your name is Hello");

    flash.error("Your name is %s", "Hello %");
    assertThat(flash.out.get("error")).isEqualTo("Your name is Hello %");

    Messages.defaults = new Properties();
    Messages.defaults.setProperty("your.name.label", "Your name is %s");
    flash.error("your.name.label", "Hello");

    assertThat(flash.out.get("error")).isEqualTo("Your name is Hello");

    flash.error("your.name.label", "Hello %");
    assertThat(flash.out.get("error")).isEqualTo("Your name is Hello %");
  }

  @Test
  public void flashSuccessFormat() {
    Flash flash = new Flash();
    flash.success("Your name is %s", "Hello");

    assertThat(flash.out.get("success")).isEqualTo("Your name is Hello");

    flash.success("Your name is %s", "Hello %");
    assertThat(flash.out.get("success")).isEqualTo("Your name is Hello %");

    Messages.defaults = new Properties();
    Messages.defaults.setProperty("your.name.label", "Your name is %s");
    flash.success("your.name.label", "Hello");

    assertThat(flash.out.get("success")).isEqualTo("Your name is Hello");

    flash.success("your.name.label", "Hello %");
    assertThat(flash.out.get("success")).isEqualTo("Your name is Hello %");
  }

  @Test
  public void flashPut() {
    Flash flash = new Flash();

    flash.put("string", "value");
    assertThat(flash.get("string")).isEqualTo("value");

    flash.put("integer", Integer.MAX_VALUE);
    assertThat(flash.get("integer")).isEqualTo("2147483647");

    flash.put("long", Long.MAX_VALUE);
    assertThat(flash.get("long")).isEqualTo("9223372036854775807");

    flash.put("bigDecimal", new BigDecimal("12.34"));
    assertThat(flash.get("bigDecimal")).isEqualTo("12.34");

    flash.put("booleanTrue", true);
    assertThat(flash.get("booleanTrue")).isEqualTo("true");

    flash.put("booleanFalse", false);
    assertThat(flash.get("booleanFalse")).isEqualTo("false");

    flash.put("enum", TestEnum.B);
    assertThat(flash.get("enum")).isEqualTo("B");
  }

  @Test
  public void flashPutNulls() {
    Flash flash = new Flash();

    flash.put("string", (String) null);
    assertThat(flash.get("string")).isNull();

    flash.put("integer", (Integer) null);
    assertThat(flash.get("integer")).isNull();

    flash.put("long", (Long) null);
    assertThat(flash.get("long")).isNull();

    flash.put("bigDecimal", (BigDecimal) null);
    assertThat(flash.get("bigDecimal")).isNull();

    flash.put("boolean", (Boolean) null);
    assertThat(flash.get("boolean")).isNull();
  }

  private enum TestEnum {
    A,
    B;

    @Override
    public String toString() {
      return "to string";
    }
  }

  @Test
  public void containsReturnsTrueWhenOnlyParameterNameIsQueryString() {
    request.querystring = "&name&name2";
    Params params = new Params(request);
    assertThat(params.contains("name")).isTrue();
    assertThat(params.contains("name2")).isTrue();
    assertThat(params.contains("name3")).isFalse();
  }

  @Test
  public void containsFiles() {
    request.args.put("__UPLOADS", "file");
    assertThat(request.params.containsFiles()).isTrue();
  }

  @Test
  public void containsFiles_false() {
    assertThat(request.params.containsFiles()).isFalse();
  }
}
