package play.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.exceptions.UnexpectedException;
import play.libs.Signer;
import play.mvc.results.Forbidden;

import jakarta.inject.Singleton;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static play.mvc.Scope.COOKIE_PREFIX;
import static play.mvc.Scope.COOKIE_SECURE;
import static play.mvc.Scope.SESSION_HTTPONLY;

@Singleton
public class FlashStore {
  private static final Logger logger = LoggerFactory.getLogger(FlashStore.class);

  private final SessionDataEncoder encoder = new SessionDataEncoder();
  private final Signer signer;

  public FlashStore() {
    this(new Signer("флэшрояль"));
  }

  FlashStore(@Nonnull Signer signer) {
    this.signer = signer;
  }

  @Nonnull
  public Scope.Flash restore(@Nonnull Http.Request request) {
      Http.Cookie cookie = request.cookies.get(COOKIE_PREFIX + "_FLASH");
      if (cookie == null) return new Scope.Flash();

      int splitterPosition = cookie.value.indexOf('-');
      if (splitterPosition == -1) {
          logger.warn("Flash cookie without signature: '{}'", cookie.value);
          throw new Forbidden("Flash cookie without signature");
      }

      String signature = cookie.value.substring(0, splitterPosition);
      String base64EncodedContent = cookie.value.substring(splitterPosition + 1);
      if (!signer.isValid(signature, base64EncodedContent)) {
          logger.warn("Invalid flash cookie signature on {} (decoded content: '{}')",
              cookie.value,
              encoder.decode(base64EncodedContent));
          throw new Forbidden("Invalid flash cookie signature");
      }
      return new Scope.Flash(encoder.decode(base64EncodedContent));
  }

  public void save(@Nonnull Scope.Flash flash, @Nonnull Http.Request request, @Nullable Http.Response response) {
      if (response == null) {
          // Some requests like WebSocket requests don't have a response
          return;
      }

      warnIfFlashIsGone(flash, request);

      if (flash.out.isEmpty()) {
          if (request.cookies.containsKey(COOKIE_PREFIX + "_FLASH")) {
              response.setCookie(COOKIE_PREFIX + "_FLASH", "", null, "/", 0, COOKIE_SECURE, SESSION_HTTPONLY);
          }
          return;
      }
      try {
          String flashData = encoder.encode(flash.out);
          int maximumAcceptableCookieLength = 3980;
          if (flashData.length() > maximumAcceptableCookieLength) {
              logger.error("Too long flash ({}): {}", flashData.length(), flashData);
          }
          else {
              int recommendedMaximumCookieLength = 2000;
              if (flashData.length() > recommendedMaximumCookieLength) {
                  logger.warn("Flash size {} bytes, recommending to redesign the page {} to avoid overusing of flash. Flash content: {}",
                      flashData.length(), request.path, flash.out);
              }
          }
          String signature = signer.sign(flashData);
          response.setCookie(COOKIE_PREFIX + "_FLASH", signature + '-' + flashData, null, "/", null, COOKIE_SECURE, SESSION_HTTPONLY);
      } catch (Exception e) {
          throw new UnexpectedException("Flash serializationProblem", e);
      }
  }

  private void warnIfFlashIsGone(@Nonnull Scope.Flash flash, @Nonnull Http.Request request) {
    for (Map.Entry<String, String> entry : flash.data.entrySet()) {
      if (!flash.out.containsKey(entry.getKey()) && !flash.used.contains(entry.getKey())) {
        logger.debug("Unused flash param: {}={} in request {}", entry.getKey(), entry.getValue(), request.path);
      }
    }
  }
}
