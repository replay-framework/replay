package services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.CriminalRecord;
import org.apache.http.client.fluent.Request;

import javax.inject.Singleton;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class RestClient {
  public List<CriminalRecord> get(String url) throws IOException {
    String jsonResponse = Request.Get(url).execute().returnContent().asString();

    Type criminalRecordType = new TypeToken<CriminalRecord>() {}.getType();
    Type criminalRecordListType = TypeToken.getParameterized(ArrayList.class, criminalRecordType).getType();
    return new Gson().fromJson(jsonResponse, criminalRecordListType);
  }
}
