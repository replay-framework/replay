package services;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import models.CriminalRecord;
import models.Verdict;
import org.junit.jupiter.api.Test;

public class CriminalSafetyCalculatorTest {
  RestClient restClient = mock(RestClient.class);
  CriminalSafetyCalculator service =
      new CriminalSafetyCalculator(restClient, "http://x.y.z/service");

  @Test
  public void isSafeIfCriminalHistoryIsEmpty() throws IOException {
    // arrange
    when(restClient.get(anyString())).thenReturn(emptyList());

    // act
    Verdict verdict = service.check("111222333");

    // assert
    assertThat(verdict.canBeFree).isTrue();
    assertThat(verdict.explanation)
        .isEqualTo("криминальная история чиста. можно выпускать на волю.");
    verify(restClient).get("http://x.y.z/service?ssn=111222333");
  }

  @Test
  public void isUnsafeIfCriminalHistoryIsPresent() throws IOException {
    // arrange
    when(restClient.get(anyString())).thenReturn(List.of(new CriminalRecord("убийство")));

    // act
    Verdict verdict = service.check("111222333");

    // assert
    assertThat(verdict.canBeFree).isFalse();
    assertThat(verdict.explanation).isEqualTo("обнаружен криминал. нельзя выпускать на волю.");
    verify(restClient).get("http://x.y.z/service?ssn=111222333");
  }

  @Test
  public void throwsExceptionIfCouldNotCheckHistory() throws IOException {
    // arrange
    when(restClient.get(anyString())).thenThrow(new IOException("could not connect"));

    // act
    Verdict verdict = service.check("111222333");

    // assert
    assertThat(verdict.canBeFree).isFalse();
    assertThat(verdict.explanation)
        .isEqualTo("не удалось проверить историю преступлений. Нельзя выпускать на волю.");
    verify(restClient).get("http://x.y.z/service?ssn=111222333");
  }
}
