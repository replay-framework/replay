package play.libs.ws;

public interface WSClient {
    WSRequest newRequest(String url);

    void stop();
}
