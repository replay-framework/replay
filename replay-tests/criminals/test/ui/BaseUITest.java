package ui;

import static com.codeborne.selenide.TextCheck.FULL_TEXT;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.codeborne.selenide.Configuration;
import com.github.tomakehurst.wiremock.WireMockServer;
import criminals.Application;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.fontbox.FontBoxFont;
import org.apache.pdfbox.pdmodel.font.FontMapper;
import org.apache.pdfbox.pdmodel.font.FontMapping;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;

public class BaseUITest {
  protected static final WireMockServer wireMock = new WireMockServer(0);
  private static final Logger LOG = LoggerFactory.getLogger(BaseUITest.class);
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ScheduledExecutorService job = newScheduledThreadPool(1);
  private final String prefix = new SimpleDateFormat("HH-mm-ss-SSS").format(new Date());

  @BeforeAll
  public static void preloadPdfFonts() {
    long startTime = currentTimeMillis();
    FontMapper mapper = org.apache.pdfbox.pdmodel.font.FontMappers.instance();
    FontMapping<FontBoxFont> sampleFont = mapper.getFontBoxFont("Helvetica", null);
    LOG.info("Loaded font mapper {} and sample font {} in {} ms.", mapper, sampleFont, currentTimeMillis() - startTime);
  }

  @BeforeEach
  public final void startAUT() {
    if (!Play.started) {
      log.info("Starting AUT with classpath {}", System.getProperty("java.class.path"));

      wireMock.start();
      String criminalRecordsServiceUrl =
          String.format("http://127.0.0.1:%s/criminal-records", wireMock.port());
      int port = new Application().start("test", criminalRecordsServiceUrl);

      Configuration.baseUrl = "http://127.0.0.1:" + port;
      Configuration.browserSize = "1024x800";
      Configuration.textCheck = FULL_TEXT;

      log.info("Started AUT at {}", Configuration.baseUrl);
    } else {
      log.info("Running AUT on {}", Configuration.baseUrl);
    }
  }

  @BeforeEach
  final void startThreadDumper() {
    log.info("Saving thread dumps in files build/reports/thread-dump-{}-*", prefix);
    job.scheduleWithFixedDelay(() -> saveThreadDump(), 5_000, 100, MILLISECONDS);
  }

  @AfterEach
  final void stopThreadDumper() throws InterruptedException {
    long start = currentTimeMillis();
    job.shutdown();
    boolean terminated = job.awaitTermination(5, SECONDS);
    job.shutdownNow();
    log.info("Stopped thread dumper {} in {} ms: {}", prefix, currentTimeMillis() - start, terminated);
  }

  private void saveThreadDump() {
    File build = new File("build");
    build.mkdirs();
    File reports = new File(build, "reports");
    reports.mkdirs();

    File file = new File(reports, "thread-dump-%s-%s.txt".formatted(prefix, currentTimeMillis()));

    ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] threadInfos = threadMxBean.dumpAllThreads(true, true);

    try (FileWriter writer = new FileWriter(file, UTF_8)) {
      for (ThreadInfo threadInfo : threadInfos) {
        writer.write(threadInfo.toString());
        for (StackTraceElement ste : threadInfo.getStackTrace()) {
          writer.write("\tat " + ste.toString() + "\n");
        }
        writer.write("\n");
      }
    }
    catch (IOException e) {
      log.error("Failed to dump threads to file {}: {}", file, e.toString());
    }
  }
}
