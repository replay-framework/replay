package play.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.exceptions.UnexpectedException;
import play.libs.Signer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

import static play.mvc.Scope.COOKIE_PREFIX;
import static play.mvc.Scope.COOKIE_SECURE;
import static play.mvc.Scope.SESSION_HTTPONLY;
import static play.mvc.Scope.SESSION_SEND_ONLY_IF_CHANGED;

@Singleton
public class FlashStore {
  private static final Logger logger = LoggerFactory.getLogger(FlashStore.class);

  private final SessionDataEncoder encoder = new SessionDataEncoder();
  private final Signer signer;

  public FlashStore() {
    this(new Signer("флэшрояль"));
  }

  FlashStore(Signer signer) {
    this.signer = signer;
  }

  @Nonnull
  public Scope.Flash restore(@Nonnull Http.Request request) {
      Http.Cookie cookie = request.cookies.get(COOKIE_PREFIX + "_FLASH");
      if (cookie == null) {
        return new Scope.Flash();
      }
      int splitterPosition = cookie.value.indexOf('-');
      if (splitterPosition == -1) {
          logger.warn("Cookie without signature: {}", cookie.value);
        return new Scope.Flash();
      }

      String signature = cookie.value.substring(0, splitterPosition);
      String realValue = cookie.value.substring(splitterPosition + 1);
      if (!signer.isValid(signature, realValue)) {
          throw new ForbiddenException(String.format("Invalid flash signature: %s", cookie.value));
      }
      return new Scope.Flash(encoder.decode(realValue));
  }

  public void save(@Nonnull Scope.Flash flash, @Nonnull Http.Request request, @Nullable Http.Response response) {
      if (response == null) {
          // Some request like WebSocket don't have any response
          return;
      }
      if (flash.out.isEmpty()) {
          if (request.cookies.containsKey(COOKIE_PREFIX + "_FLASH") || !SESSION_SEND_ONLY_IF_CHANGED) {
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
}
