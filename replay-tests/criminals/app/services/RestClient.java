package services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import models.CriminalRecord;
import org.apache.http.client.fluent.Request;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;

@Singleton
public class RestClient {
  public  <T> List<T> get(String url) throws IOException {
    String jsonResponse = Request.Get(url).execute().returnContent().asString();
    return new Gson().fromJson(jsonResponse, new TypeToken<List<T>>() {}.getType());
  }
}
