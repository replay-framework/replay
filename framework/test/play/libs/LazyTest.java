package play.libs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static play.libs.Lazy.lazyEvaluated;

class LazyTest {

  @Test
  void evaluatesValueOnlyOnce() {
    AtomicInteger counter = new AtomicInteger(8);
    Lazy<Integer> value = lazyEvaluated(() -> counter.incrementAndGet());

    assertThat(value.get()).isEqualTo(9);
    assertThat(value.get()).isEqualTo(9);
    assertThat(value.get()).isEqualTo(9);
    assertThat(counter.get()).isEqualTo(9);
    assertThat(value.isInitialized()).isTrue();
  }

  @Test
  void doesNotEvaluateValueUntilAsked() {
    AtomicInteger counter = new AtomicInteger(8);
    Lazy<Integer> value = lazyEvaluated(() -> counter.incrementAndGet());

    assertThat(value.isInitialized()).isFalse();
    assertThat(counter.get()).isEqualTo(8);
  }
}