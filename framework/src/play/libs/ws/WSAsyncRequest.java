package play.libs.ws;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Realm;
import com.ning.http.client.Response;
import com.ning.http.client.multipart.ByteArrayPart;
import com.ning.http.client.multipart.FilePart;
import com.ning.http.client.multipart.Part;
import java.io.InputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.lang3.NotImplementedException;
import play.libs.MimeTypes;
import play.libs.Promise;

class WSAsyncRequest extends WSRequest {

  private final AsyncHttpClient httpClient;
  protected String type;
  private String generatedContentType;

  protected WSAsyncRequest(AsyncHttpClient httpClient, String url, Charset encoding) {
    super(url, encoding);
    this.httpClient = httpClient;
  }

  /**
   * Returns the URL but removed the queryString-part of it The QueryString-info is later added with addQueryString().
   *
   * @return The URL without the queryString-part
   */
  protected String getUrlWithoutQueryString() {
    int i = url.indexOf('?');
    if (i > 0) {
      return url.substring(0, i);
    } else {
      return url;
    }
  }

  /**
   * Adds the queryString-part of the url to the BoundRequestBuilder
   *
   * @param requestBuilder The request builder to add the queryString-part
   */
  protected void addQueryString(AsyncHttpClient.BoundRequestBuilder requestBuilder) {

    // AsyncHttpClient is by default encoding everything in UTF-8. So for us to be able to use a different encoding we
    // have configured AsyncHttpClient to use raw urls. When using raw urls, AsyncHttpClient does not encode url and
    // QueryParam with utf-8 - but there is another problem:
    // If we send raw (none-encoded) url (with queryString) to AsyncHttpClient, it does not url-encode it, but it
    // transforms all illegal chars to '?'. If we pre-encoded the url with QueryString before sending it to
    // AsyncHttpClient, it will decode it, and then later break it with '?'.

    // This method basically does the same as RequestBuilderBase.buildUrl() except from destroying the pre-encoding.

    int i = url.indexOf('?');
    // Only do this if the url contains one or more query parameters.
    if (i > 0) {

      // Extract query-parameters-part
      String queryPart = url.substring(i + 1);

      // Parse the query part and decode it... (it is going to be re-encoded later)
      for (String param : queryPart.split("&")) {

        i = param.indexOf('=');
        String name;
        String value = null;
        if (i <= 0) {
          // `name` is only a flag
          name = URLDecoder.decode(param, encoding);
        } else {
          name = URLDecoder.decode(param.substring(0, i), encoding);
          value = URLDecoder.decode(param.substring(i + 1), encoding);
        }

        if (value == null) {
          requestBuilder.addQueryParam(URLEncoder.encode(name, encoding), null);
        } else {
          requestBuilder.addQueryParam(URLEncoder.encode(name, encoding),
              URLEncoder.encode(value, encoding));
        }
      }
    }
  }

  private AsyncHttpClient.BoundRequestBuilder prepareAll(
      AsyncHttpClient.BoundRequestBuilder requestBuilder) {
    checkFileBody(requestBuilder);
    addQueryString(requestBuilder);
    addGeneratedContentType(requestBuilder);
    return requestBuilder;
  }

  public AsyncHttpClient.BoundRequestBuilder prepareGet() {
    return prepareAll(httpClient.prepareGet(getUrlWithoutQueryString()));
  }

  public AsyncHttpClient.BoundRequestBuilder prepareOptions() {
    return prepareAll(httpClient.prepareOptions(getUrlWithoutQueryString()));
  }

  public AsyncHttpClient.BoundRequestBuilder prepareHead() {
    return prepareAll(httpClient.prepareHead(getUrlWithoutQueryString()));
  }

  public AsyncHttpClient.BoundRequestBuilder preparePatch() {
    return prepareAll(httpClient.preparePatch(getUrlWithoutQueryString()));
  }

  public AsyncHttpClient.BoundRequestBuilder preparePost() {
    return prepareAll(httpClient.preparePost(getUrlWithoutQueryString()));
  }

  public AsyncHttpClient.BoundRequestBuilder preparePut() {
    return prepareAll(httpClient.preparePut(getUrlWithoutQueryString()));
  }

  public AsyncHttpClient.BoundRequestBuilder prepareDelete() {
    return prepareAll(httpClient.prepareDelete(getUrlWithoutQueryString()));
  }

  /**
   * Execute a GET request synchronously.
   */
  @Override
  public HttpResponse get() {
    this.type = "GET";
    try {
      return new HttpAsyncResponse(prepare(prepareGet()).execute().get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Execute a GET request asynchronously.
   */
  @Override
  public Promise<HttpResponse> getAsync() {
    this.type = "GET";
    return execute(prepareGet());
  }

  /**
   * Execute a PATCH request.
   */
  @Override
  public HttpResponse patch() {
    this.type = "PATCH";
    try {
      return new HttpAsyncResponse(prepare(preparePatch()).execute().get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Execute a PATCH request asynchronously.
   */
  @Override
  public Promise<HttpResponse> patchAsync() {
    this.type = "PATCH";
    return execute(preparePatch());
  }

  /**
   * Execute a POST request.
   */
  @Override
  public HttpResponse post() {
    this.type = "POST";
    try {
      return new HttpAsyncResponse(prepare(preparePost()).execute().get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Execute a POST request asynchronously.
   */
  @Override
  public Promise<HttpResponse> postAsync() {
    this.type = "POST";
    return execute(preparePost());
  }

  /**
   * Execute a PUT request.
   */
  @Override
  public HttpResponse put() {
    this.type = "PUT";
    try {
      return new HttpAsyncResponse(prepare(preparePut()).execute().get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Execute a PUT request asynchronously.
   */
  @Override
  public Promise<HttpResponse> putAsync() {
    this.type = "PUT";
    return execute(preparePut());
  }

  /**
   * Execute a DELETE request.
   */
  @Override
  public HttpResponse delete() {
    this.type = "DELETE";
    try {
      return new HttpAsyncResponse(prepare(prepareDelete()).execute().get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Execute a DELETE request asynchronously.
   */
  @Override
  public Promise<HttpResponse> deleteAsync() {
    this.type = "DELETE";
    return execute(prepareDelete());
  }

  /**
   * Execute a OPTIONS request.
   */
  @Override
  public HttpResponse options() {
    this.type = "OPTIONS";
    try {
      return new HttpAsyncResponse(prepare(prepareOptions()).execute().get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Execute a OPTIONS request asynchronously.
   */
  @Override
  public Promise<HttpResponse> optionsAsync() {
    this.type = "OPTIONS";
    return execute(prepareOptions());
  }

  /**
   * Execute a HEAD request.
   */
  @Override
  public HttpResponse head() {
    this.type = "HEAD";
    try {
      return new HttpAsyncResponse(prepare(prepareHead()).execute().get());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Execute a HEAD request asynchronously.
   */
  @Override
  public Promise<HttpResponse> headAsync() {
    this.type = "HEAD";
    return execute(prepareHead());
  }

  /**
   * Execute a TRACE request.
   */
  @Override
  public HttpResponse trace() {
    this.type = "TRACE";
    throw new NotImplementedException();
  }

  /**
   * Execute a TRACE request asynchronously.
   */
  @Override
  public Promise<HttpResponse> traceAsync() {
    this.type = "TRACE";
    throw new NotImplementedException();
  }

  private AsyncHttpClient.BoundRequestBuilder prepare(AsyncHttpClient.BoundRequestBuilder builder) {
    if (this.username != null && this.password != null && this.scheme != null) {
      Realm.AuthScheme authScheme = switch (this.scheme) {
        case DIGEST -> Realm.AuthScheme.DIGEST;
        case NTLM -> Realm.AuthScheme.NTLM;
        case KERBEROS -> Realm.AuthScheme.KERBEROS;
        case SPNEGO -> Realm.AuthScheme.SPNEGO;
        case BASIC -> Realm.AuthScheme.BASIC;
      };
      builder.setRealm(
          (new Realm.RealmBuilder())
              .setScheme(authScheme)
              .setPrincipal(this.username)
              .setPassword(this.password)
              .setUsePreemptiveAuth(true)
              .build());
    }
    for (Map.Entry<String, String> entry : this.headers.entrySet()) {
      builder.addHeader(entry.getKey(), entry.getValue());
    }
    builder.setFollowRedirects(this.followRedirects);
    builder.setRequestTimeout(this.timeout * 1000);
    if (this.virtualHost != null) {
      builder.setVirtualHost(this.virtualHost);
    }
    return builder;
  }

  private Promise<HttpResponse> execute(AsyncHttpClient.BoundRequestBuilder builder) {
    try {
      final Promise<HttpResponse> smartFuture = new Promise<>();
      prepare(builder)
          .execute(
              new AsyncCompletionHandler<HttpResponse>() {
                @Override
                public HttpResponse onCompleted(Response response) {
                  HttpResponse httpResponse = new HttpAsyncResponse(response);
                  smartFuture.accept(httpResponse);
                  return httpResponse;
                }

                @Override
                public void onThrowable(Throwable t) {
                  // An error happened, here the exception is being "forwarded" to the future that's awaiting the result
                  smartFuture.invokeWithException(t);
                }
              });

      return smartFuture;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void checkFileBody(AsyncHttpClient.BoundRequestBuilder builder) {
    setResolvedContentType(null);
    if (this.fileParams != null) {
      // Could be optimized, we know the size of this array.
      for (FileParam fileParam : this.fileParams) {
        builder.addBodyPart(
            new FilePart(
                fileParam.paramName,
                fileParam.file,
                MimeTypes.getMimeType(fileParam.file.getName()),
                encoding));
      }
      // AsyncHttpClient only supports ASCII chars in keys in multipart
      for (Map.Entry<String, Object> entry : this.parameters.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();
        if (value instanceof Collection<?> || value.getClass().isArray()) {
          Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
          for (Object v : values) {
            Part part = new ByteArrayPart(
                key,
                v.toString().getBytes(encoding),
                "text/plain",
                encoding,
                null);
            builder.addBodyPart(part);
          }
        } else {
          Part part = new ByteArrayPart(
              key,
              value.toString().getBytes(encoding),
              "text/plain",
              encoding,
              null);
          builder.addBodyPart(part);
        }
      }

      // Don't have to set content-type: AsyncHttpClient will automatically choose multipart

      return;
    }
    if (!this.parameters.isEmpty()) {
      boolean isPostPut = "POST".equals(this.type) || ("PUT".equals(this.type));

      if (isPostPut) {
        // Since AsyncHttpClient is hard-coded to encode to use UTF-8, we must build the content ourselves
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Object> entry : this.parameters.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();
          if (value == null) {
            continue;
          }

          if (value instanceof Collection<?> || value.getClass().isArray()) {
            Collection<?> values = value.getClass().isArray() ? Arrays.asList((Object[]) value) : (Collection<?>) value;
            for (Object v : values) {
              if (!sb.isEmpty()) {
                sb.append('&');
              }
              sb.append(encode(key));
              sb.append('=');
              sb.append(encode(v.toString()));
            }
          } else {
            // Since AsyncHttpClient is hard-coded to encode using UTF-8, we must build the content ourselves.
            if (!sb.isEmpty()) {
              sb.append('&');
            }
            sb.append(encode(key));
            sb.append('=');
            sb.append(encode(value.toString()));
          }
        }
        byte[] bodyBytes = sb.toString().getBytes(this.encoding);
        builder.setBody(bodyBytes);

        setResolvedContentType("application/x-www-form-urlencoded; charset=" + encoding);

      } else {
        for (Map.Entry<String, Object> entry : this.parameters.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();
          if (value == null) {
            continue;
          }
          if (value instanceof Collection<?> || value.getClass().isArray()) {
            Collection<?> values = value.getClass().isArray()
                ? Arrays.asList((Object[]) value)
                : (Collection<?>) value;
            for (Object v : values) {
              // Must encode it since AsyncHttpClient uses raw urls
              builder.addQueryParam(encode(key), encode(v.toString()));
            }
          } else {
            // Must encode it since AsyncHttpClient uses raw urls
            builder.addQueryParam(encode(key), encode(value.toString()));
          }
        }
        setResolvedContentType("text/html; charset=" + encoding);
      }
    }
    if (this.body != null) {
      if (!this.parameters.isEmpty()) {
        throw new RuntimeException("POST or PUT method with parameters AND body are not supported.");
      }
      if (this.body instanceof InputStream) {
        builder.setBody((InputStream) this.body);
      } else {
        byte[] bodyBytes = this.body.toString().getBytes(this.encoding);
        builder.setBody(bodyBytes);
      }
      setResolvedContentType("text/html; charset=" + encoding);
    }

    if (this.mimeType != null) {
      // The requester has specified mimeType
      this.headers.put("Content-Type", this.mimeType);
    }
  }

  /**
   * Sets the resolved Content-type. This is added as Content-type-header to AsyncHttpClient if the requester has not
   * specified Content-type or mimeType explicitly. Cannot add it directly to this.header since this causes problems
   * when Request-object is used multiple times, e.g.: first GET, then POST.
   */
  private void setResolvedContentType(String contentType) {
    generatedContentType = contentType;
  }

  /**
   * If generatedContentType is present AND if Content-type header is not already present, add generatedContentType as
   * Content-Type to headers in requestBuilder.
   */
  private void addGeneratedContentType(AsyncHttpClient.BoundRequestBuilder requestBuilder) {
    if (!headers.containsKey("Content-Type") && generatedContentType != null) {
      requestBuilder.addHeader("Content-Type", generatedContentType);
    }
  }
}
