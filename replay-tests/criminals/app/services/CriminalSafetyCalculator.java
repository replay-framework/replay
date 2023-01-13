package services;

import com.google.inject.name.Named;
import models.CriminalRecord;
import models.Verdict;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;

@Singleton
public class CriminalSafetyCalculator {
  private static final Logger logger = LoggerFactory.getLogger(CriminalSafetyCalculator.class);

  private final RestClient restClient;
  private final String serviceUrl;

  @Inject
  CriminalSafetyCalculator(RestClient restClient, @Named("criminal-records.service.url") String serviceUrl) {
    this.restClient = restClient;
    this.serviceUrl = serviceUrl;
  }

  public Verdict check(String ssn) {
    try {
      List<CriminalRecord> criminalRecords = restClient.get(serviceUrl + "?ssn=" + ssn);
      String explanation = criminalRecords.isEmpty() ?
          "криминальная история чиста. можно выпускать на волю." :
          "обнаружен криминал. нельзя выпускать на волю.";
      return new Verdict(criminalRecords.isEmpty(), explanation);
    } catch (IOException e) {
      logger.error("Failed to check criminal history", e);
      return new Verdict(false, "не удалось проверить историю преступлений. Нельзя выпускать на волю.");
    }
  }
}
