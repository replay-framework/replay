package play.libs;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class CronExpressionTest {
  @Test
  public void sample() throws ParseException {
    CronExpression cron = new CronExpression("*/6 19/20 * * * ?");
    assertThat(cron.expressionParsed).isTrue();
    assertThat(cron.seconds).containsExactly(0, 6, 12, 18, 24, 30, 36, 42, 48, 54);
    assertThat(cron.minutes).containsExactly(19, 39, 59);
    assertThat(cron.hours).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 99);
    assertThat(cron.daysOfMonth).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 99);
    assertThat(cron.months).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 99);
    assertThat(cron.daysOfWeek).as("why 98?").containsExactly(98);
    assertThat(cron.years).contains(1970, 1981, 2021, 2022, 2023, 2090);
    assertThat(cron.lastdayOfWeek).isFalse();
    assertThat(cron.nthdayOfWeek).isEqualTo(0);
    assertThat(cron.lastdayOfMonth).isFalse();
    assertThat(cron.nearestWeekday).isFalse();
  }

  @Test
  public void getNextValidTimeAfter() throws ParseException {
    CronExpression cron = new CronExpression("*/6 19/20 * * * ?");

    assertThat(cron.getNextValidTimeAfter(date("2023-04-08 13:51:01"))).isEqualTo(date("2023-04-08 13:59:00"));
    assertThat(cron.getNextValidTimeAfter(date("2023-04-08 13:59:01"))).isEqualTo(date("2023-04-08 13:59:06"));
  }

  private static Date date(String date) throws ParseException {
    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date);
  }
}