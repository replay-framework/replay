package play.libs.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Play;
import play.libs.IO;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSImpl;
import play.libs.WS.WSRequest;
import play.mvc.Http.Header;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implementation of the WS interface based on Java URL Fetch API. This is to be used for example in Google App Engine,
 * where the async http client can't be used.
 */
public class WSUrlFetch implements WSImpl {
    private static final Logger logger = LoggerFactory.getLogger(WSUrlFetch.class);

    private static SSLContext sslCTX = null;

    public WSUrlFetch() {
    }

    @Override
    public void stop() {
    }

    @Override
    public play.libs.WS.WSRequest newRequest(String url, String encoding) {
        return new WSUrlfetchRequest(url, encoding);
    }

    public class WSUrlfetchRequest extends WSRequest {

        protected WSUrlfetchRequest(String url, String encoding) {
            super(url, encoding);
        }

        private String getPreparedUrl(String method) {
            String u = url;
            if (parameters != null && !parameters.isEmpty()) {
                // If not PUT or POST, we must add these params to the
                // queryString
                if (!("PUT".equals(method) || "POST".equals(method))) {
                    // must add params to queryString/url
                    StringBuilder sb = new StringBuilder(url);
                    if (url.indexOf("?") > 0) {
                        sb.append('&');
                    } else {
                        sb.append('?');
                    }
                    int count = 0;
                    for (Map.Entry<String, Object> e : parameters.entrySet()) {
                        count++;
                        String key = e.getKey();
                        Object value = e.getValue();
                        if (value == null) {
                            continue;
                        }

                        if (value instanceof Collection<?> || value.getClass().isArray()) {
                            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
                            for (Object v : values) {
                                if (count > 1) {
                                    sb.append('&');
                                }
                                sb.append(encode(key));
                                sb.append('=');
                                sb.append(encode(v.toString()));
                            }
                        } else {
                            if (count > 1) {
                                sb.append('&');
                            }
                            sb.append(encode(key));
                            sb.append('=');
                            sb.append(encode(value.toString()));
                        }

                    }

                    u = sb.toString();

                    // Must clear the parameters to prevent us from using them
                    // again
                    parameters.clear();
                }
            }
            return u;
        }

        /** Execute a GET request synchronously. */
        @Override
        public HttpResponse get() {
            try {
                return new HttpUrlFetchResponse(prepare(new URL(getPreparedUrl("GET")), "GET"));
            } catch (Exception e) {
                logger.error(e.toString());
                throw new RuntimeException(e);
            }
        }

        @Override
        public HttpResponse patch() {
            try {
                HttpURLConnection conn = prepare(new URL(getPreparedUrl("PATCH")), "PATCH");
                return new HttpUrlFetchResponse(conn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a POST request. */
        @Override
        public HttpResponse post() {
            try {
                HttpURLConnection conn = prepare(new URL(getPreparedUrl("POST")), "POST");
                return new HttpUrlFetchResponse(conn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a PUT request. */
        @Override
        public HttpResponse put() {
            try {
                return new HttpUrlFetchResponse(prepare(new URL(getPreparedUrl("PUT")), "PUT"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a DELETE request. */
        @Override
        public HttpResponse delete() {
            try {
                return new HttpUrlFetchResponse(prepare(new URL(getPreparedUrl("DELETE")), "DELETE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a OPTIONS request. */
        @Override
        public HttpResponse options() {
            try {
                return new HttpUrlFetchResponse(prepare(new URL(getPreparedUrl("OPTIONS")), "OPTIONS"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a HEAD request. */
        @Override
        public HttpResponse head() {
            try {
                return new HttpUrlFetchResponse(prepare(new URL(getPreparedUrl("HEAD")), "HEAD"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        /** Execute a TRACE request. */
        @Override
        public HttpResponse trace() {
            try {
                return new HttpUrlFetchResponse(prepare(new URL(getPreparedUrl("TRACE")), "TRACE"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private HttpURLConnection prepare(URL url, String method) {
            String keyStore = Play.configuration.getProperty("ssl.keyStore", System.getProperty("javax.net.ssl.keyStore"));
            String keyStorePass = Play.configuration.getProperty("ssl.keyStorePassword",
                    System.getProperty("javax.net.ssl.keyStorePassword"));
            Boolean CAValidation = Boolean.parseBoolean(Play.configuration.getProperty("ssl.cavalidation", "true"));

            if (this.username != null && this.password != null && this.scheme != null) {
                String authString = null;
                switch (this.scheme) {
                case BASIC:
                    authString = basicAuthHeader();
                    break;
                default:
                    throw new RuntimeException("Scheme " + this.scheme + " not supported by the UrlFetch WS backend.");
                }
                this.headers.put("Authorization", authString);
            }

            if (keyStore != null && !keyStore.equals("")) {
                logger.info("Keystore configured, loading from '{}', CA validation enabled : {}", keyStore, CAValidation);
                logger.trace("Keystore password : {}, SSLCTX : {}", keyStorePass, sslCTX);

                if (sslCTX == null) {
                    sslCTX = WSSSLContext.getSslContext(keyStore, keyStorePass, CAValidation);
                }
            }

            try {
                URLConnection connection = url.openConnection();
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection cssl = (HttpsURLConnection) connection;
                    if (sslCTX != null) {
                        SSLSocketFactory sslSocketFactory = sslCTX.getSocketFactory();
                        cssl.setSSLSocketFactory(sslSocketFactory);
                    }
                    cssl.setRequestMethod(method);
                    cssl.setInstanceFollowRedirects(this.followRedirects);
                } else {
                    HttpURLConnection c = (HttpURLConnection) connection;
                    c.setRequestMethod(method);
                    c.setInstanceFollowRedirects(this.followRedirects);
                }

                connection.setDoInput(true);
                connection.setReadTimeout(this.timeout * 1000);
                for (String key : this.headers.keySet()) {
                    connection.setRequestProperty(key, headers.get(key));
                }

                checkFileBody((HttpURLConnection) connection);
                return (HttpURLConnection) connection;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void checkFileBody(HttpURLConnection connection) throws IOException {
            if (this.parameters != null && !this.parameters.isEmpty()) {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + encoding);
                connection.setDoOutput(true);
                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), UTF_8)) {
                    writer.write(createQueryString());
                }
            }
            if (this.body != null) {
                if (this.parameters != null && !this.parameters.isEmpty()) {
                    throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
                }
                connection.setDoOutput(true);

                if (this.mimeType != null) {
                    connection.setRequestProperty("Content-Type", this.mimeType + "; charset=" + encoding);
                }
                OutputStream out = connection.getOutputStream();
                if (this.body instanceof InputStream) {
                    InputStream bodyStream = (InputStream) this.body;
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = bodyStream.read(buffer, 0, buffer.length)) > 0) {
                        out.write(buffer, 0, bytesRead);
                    }
                } else {
                    try {
                        byte[] bodyBytes = this.body.toString().getBytes(this.encoding);
                        out.write(bodyBytes);
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }

            }
        }
    }

    /**
     * An HTTP response wrapper
     */
    public static class HttpUrlFetchResponse extends HttpResponse {

        private String body;
        private Integer status;
        private String statusText;
        private Map<String, List<String>> headersMap;

        /**
         * you shouldn't have to create an HttpResponse yourself
         * 
         * @param connection
         *            The current connection
         */
        public HttpUrlFetchResponse(HttpURLConnection connection) {
            try {
                this.status = connection.getResponseCode();
                this.statusText = connection.getResponseMessage();
                this.headersMap = connection.getHeaderFields();
                try (InputStream is = this.status >= HttpURLConnection.HTTP_BAD_REQUEST ?
                  // 4xx/5xx may return a response via getErrorStream()
                  connection.getErrorStream() : connection.getInputStream()) {
                    if (is != null) {
                        this.body = IO.readContentAsString(is, getEncoding());
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            } finally {
                connection.disconnect();
            }
        }

        /**
         * the HTTP status code
         * 
         * @return the status code of the http response
         */
        @Override
        public Integer getStatus() {
            return status;
        }

        /**
         * the HTTP status text
         * 
         * @return the status text of the http response
         */
        @Override
        public String getStatusText() {
            return statusText;
        }

        @Override
        public String getHeader(String key) {
            return headersMap.containsKey(key) ? headersMap.get(key).get(0) : null;
        }

        @Override
        public List<Header> getHeaders() {
            List<Header> result = new ArrayList<>();
            for (String key : headersMap.keySet()) {
                result.add(new Header(key, headersMap.get(key)));
            }
            return result;
        }

        /**
         * get the response body as a string
         * 
         * @return the body of the http response
         */
        @Override
        public String getString() {
            return body;
        }

        @Override
        public String getString(String encoding) {
            try {
                return new String(body.getBytes(UTF_8), encoding);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * get the response as a stream
         * 
         * @return an inputstream
         */
        @Override
        public InputStream getStream() {
            try {
                return new ByteArrayInputStream(body.getBytes(getEncoding()));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
