package play.mvc;

import org.apache.commons.codec.binary.Base64;
import org.junit.Test;
import play.libs.Signer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class FlashStoreTest {
  private final Signer signer = mock(Signer.class);
  private final FlashStore store = new FlashStore(signer);
  private final Http.Request request = new Http.Request();
  private final Http.Response response = new Http.Response();

  @Test
  public void flash_save_addsSignatureToCookieValue() {
      Scope.Flash flash = new Scope.Flash();
      flash.put("foo", "bar");
      when(signer.sign(anyString())).thenReturn("SIGNATURE");

      store.save(flash, request, response);

      assertThat(response.cookies).containsKeys("PLAY_FLASH");
      String cookie = response.cookies.get("PLAY_FLASH").value;
      assertThat(cookie).isEqualTo("SIGNATURE-Zm9vPWJhcg==");
      assertThat(new String(Base64.decodeBase64(cookie.replace("SIGNATURE-", "")), UTF_8)).isEqualTo("foo=bar");
      verify(signer).sign("Zm9vPWJhcg==");
  }

  @Test
  public void flash_restore() {
      request.cookies.put("PLAY_FLASH", new Http.Cookie("PLAY_FLASH", "SIGNATURE-Zm9vPWJhcg=="));
      when(signer.isValid(anyString(), anyString())).thenReturn(true);

      Scope.Flash flash = store.restore(request);

      assertThat(flash.get("foo")).isEqualTo("bar");
      verify(signer).isValid("SIGNATURE", "Zm9vPWJhcg==");
  }

  @Test
  public void flash_restore_checksSignature() {
      request.cookies.put("PLAY_FLASH", new Http.Cookie("PLAY_FLASH", "SIGNATURE-Zm9vPWJhcg=="));
      when(signer.isValid(anyString(), anyString())).thenReturn(false);

      assertThatThrownBy(() -> store.restore(request))
        .isInstanceOf(ForbiddenException.class)
        .hasMessage("Invalid flash signature: SIGNATURE-Zm9vPWJhcg==");

      verify(signer).isValid("SIGNATURE", "Zm9vPWJhcg==");
  }

  @Test
  public void flash_restore_oldFormatCookie() {
      request.cookies.put("PLAY_FLASH", new Http.Cookie("PLAY_FLASH", "Zm9vPWJhcg=="));

      Scope.Flash flash = store.restore(request);

      assertThat(flash.data).isEmpty();
      assertThat(flash.out).isEmpty();
      verifyNoMoreInteractions(signer);
  }
}