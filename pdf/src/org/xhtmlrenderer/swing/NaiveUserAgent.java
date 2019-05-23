package org.xhtmlrenderer.swing;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.util.XRLog;
import play.Play;
import play.vfs.VirtualFile;

import javax.imageio.ImageIO;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedHashMap;

/**
 * <p>NaiveUserAgent is a simple implementation of {@link UserAgentCallback} which places no restrictions on what
 * XML, CSS or images are loaded, and reports visited links without any filtering. The most straightforward process
 * available in the JDK is used to load the resources in question--either using java.io or java.net classes.
 * <p>
 * <p>The NaiveUserAgent has a small cache for images,
 * the size of which (number of images) can be passed as a constructor argument. There is no automatic cleaning of
 * the cache; call {@link #shrinkImageCache()} to remove the least-accessed elements--for example, you might do this
 * when a new document is about to be loaded. The NaiveUserAgent is also a DocumentListener; if registered with a
 * source of document events (like the panel hierarchy), it will respond to the
 * {@link org.xhtmlrenderer.event.DocumentListener#documentStarted()} call and attempt to shrink its cache.
 * <p>
 * <p>This class is meant as a starting point--it will work out of the box, but you should really implement your
 * own, tuned to your application's needs.
 *
 * @author Torbjorn Gannholm
 */
public class NaiveUserAgent implements UserAgentCallback, DocumentListener {
  private static final int DEFAULT_IMAGE_CACHE_SIZE = 16;
  private static final Logger logger = LoggerFactory.getLogger(NaiveUserAgent.class);

  private final FileSearcher fileSearcher;

  /**
   * a (simple) LRU cache
   */
  protected final LinkedHashMap<String, ImageResource> _imageCache;
  private final int _imageCacheCapacity;
  private String _baseURL;


  /**
   * Creates a new instance of NaiveUserAgent with a max image cache of 16 images.
   */
  public NaiveUserAgent() {
    this(DEFAULT_IMAGE_CACHE_SIZE, new FileSearcher());
  }

  /**
   * Creates a new NaiveUserAgent with a cache of a specific size.
   *
   * @param imgCacheSize Number of images to hold in cache before LRU images are released.
   */
  public NaiveUserAgent(final int imgCacheSize) {
    this(imgCacheSize, new FileSearcher());
  }

  NaiveUserAgent(final int imgCacheSize, FileSearcher fileSearcher) {
    this.fileSearcher = fileSearcher;
    this._imageCacheCapacity = imgCacheSize;

    // note we do *not* override removeEldestEntry() here--users of this class must call shrinkImageCache().
    // that's because we don't know when is a good time to flush the cache
    this._imageCache = new LinkedHashMap<>(_imageCacheCapacity, 0.75f, true);
  }

  /**
   * If the image cache has more items than the limit specified for this class, the least-recently used will
   * be dropped from cache until it reaches the desired size.
   */
  public void shrinkImageCache() {
    int ovr = _imageCache.size() - _imageCacheCapacity;
    Iterator<String> it = _imageCache.keySet().iterator();
    while (it.hasNext() && ovr-- > 0) {
      it.next();
      it.remove();
    }
  }

  /**
   * Empties the image cache entirely.
   */
  public void clearImageCache() {
    _imageCache.clear();
  }

  /**
   * Gets a Reader for the resource identified
   *
   * @param uri PARAM
   * @return The stylesheet value
   */
  protected InputStream resolveAndOpenStream(String uri) {
    uri = resolveURI(uri);
    try {
      logger.debug("Load resource from {}", uri);
      
      URLConnection connection = new URL(uri).openConnection();

      if ("true".equals(Play.configuration.getProperty("play.pdf.ssl.acceptUnknownCertificate", "false"))) {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
          @Override public X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          @Override public void checkClientTrusted(X509Certificate[] certs, String authType) {
          }

          @Override public void checkServerTrusted(X509Certificate[] certs, String authType) {
          }
        }};

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
          @Override public boolean verify(String hostname, SSLSession session) {
            return true;
          }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

      }
      return connection.getInputStream();
    }
    catch (Exception e) {
      logger.error("bad URL given: {}", uri, e);
      XRLog.exception("bad URL given: " + uri, e);
    }
    return null;
  }

  /**
   * Retrieves the CSS located at the given URI.  It's assumed the URI does point to a CSS file--the URI will
   * be accessed (using java.io or java.net), opened, read and then passed into the CSS parser.
   * The result is packed up into an CSSResource for later consumption.
   *
   * @param uri Location of the CSS source.
   * @return A CSSResource containing the parsed CSS.
   */
  @Override public CSSResource getCSSResource(String uri) {
    return new CSSResource(resolveAndOpenStream(uri));
  }

  /**
   * Retrieves the image located at the given URI. It's assumed the URI does point to an image--the URI will
   * be accessed (using java.io or java.net), opened, read and then passed into the JDK image-parsing routines.
   * The result is packed up into an ImageResource for later consumption.
   *
   * @param uri Location of the image source.
   * @return An ImageResource containing the image.
   */
  @Override public ImageResource getImageResource(String uri) {
    uri = resolveURI(uri);
    ImageResource ir = _imageCache.get(uri);
    //TODO: check that cached image is still valid
    if (ir == null) {
      InputStream is = resolveAndOpenStream(uri);
      if (is != null) {
        try {
          BufferedImage img = ImageIO.read(is);
          if (img == null) {
            throw new IOException("ImageIO.read() returned null");
          }
          ir = createImageResource(uri, img);
          _imageCache.put(uri, ir);
        }
        catch (FileNotFoundException e) {
          logger.error("Can't read image file; image at URI '{}' not found", uri, e);
          XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
        }
        catch (IOException e) {
          logger.error("Can't read image file; unexpected problem for URI '{}'", uri, e);
          XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
        }
        finally {
          try {
            is.close();
          }
          catch (IOException e) {
            // ignore
          }
        }
      }
    }
    if (ir == null) {
      ir = createImageResource(uri, null);
    }
    return ir;
  }

  /**
   * Factory method to generate ImageResources from a given Image. May be overridden in subclass.
   *
   * @param uri The URI for the image, resolved to an absolute URI.
   * @param img The image to package; may be null (for example, if image could not be loaded).
   * @return An ImageResource containing the image.
   */
  protected ImageResource createImageResource(String uri, Image img) {
    return new ImageResource(uri, AWTFSImage.createImage(img));
  }

  /**
   * Retrieves the XML located at the given URI. It's assumed the URI does point to a XML--the URI will
   * be accessed (using java.io or java.net), opened, read and then passed into the XML parser (XMLReader)
   * configured for Flying Saucer. The result is packed up into an XMLResource for later consumption.
   *
   * @param uri Location of the XML source.
   * @return An XMLResource containing the image.
   */
  @Override public XMLResource getXMLResource(String uri) {
    InputStream inputStream = resolveAndOpenStream(uri);
    try {
      return XMLResource.load(inputStream);
    }
    finally {
      if (inputStream != null) try {
        inputStream.close();
      }
      catch (IOException e) {
        // swallow
      }
    }
  }

  @Override public byte[] getBinaryResource(String uri) {
    try (InputStream is = resolveAndOpenStream(uri)) {
      return IOUtils.toByteArray(is);
    }
    catch (IOException e) {
      return null;
    }
  }


  /**
   * Returns true if the given URI was visited, meaning it was requested at some point since initialization.
   *
   * @param uri A URI which might have been visited.
   * @return Always false; visits are not tracked in the NaiveUserAgent.
   */
  @Override public boolean isVisited(String uri) {
    return false;
  }

  /**
   * URL relative to which URIs are resolved.
   *
   * @param url A URI which anchors other, possibly relative URIs.
   */
  @Override public void setBaseURL(String url) {
    _baseURL = url;
  }

  /**
   * Resolves the URI; if absolute, leaves as is, if relative, returns an absolute URI based on the baseUrl for
   * the agent.
   *
   * @param uri A URI, possibly relative.
   * @return A URI as String, resolved, or null if there was an exception (for example if the URI is malformed).
   */
  @Override public String resolveURI(String uri) {
    if (uri == null) return null;
    String ret = null;
    if (_baseURL == null) {//first try to set a base URL
      try {
        URL result = new URL(uri);
        logger.debug("Try to set base url {}", uri);
        setBaseURL(result.toExternalForm());
      }
      catch (MalformedURLException e) {
        logger.debug("Failed to set base url {} becase of {}", uri, e);
        URI newUri = new File(".").toURI();
        try {
          String newBaseUrl = newUri.toURL().toExternalForm();
          logger.debug("Try to set base url {}", newBaseUrl);
          setBaseURL(newBaseUrl);
        }
        catch (Exception e1) {
          logger.error("Failed to set base url {} becase of {}", newUri, e1);
          XRLog.exception("The default NaiveUserAgent doesn't know how to resolve the base URL for " + uri);
          return null;
        }
      }
    }
    // test if the URI is valid; if not, try to assign the base url as its parent
    try {
      // try to find it in play
      String filePath = uri.replaceFirst("\\?.*", "");
      VirtualFile file = fileSearcher.searchFor(filePath);
      logger.debug("Resolved uri {} to file {}", uri, file == null ? null : file.getRealFile().getAbsolutePath());
      if (file != null && file.exists())
        return file.getRealFile().getCanonicalFile().toURI().toURL().toExternalForm();
      return new URL(uri).toString();
    }
    catch (IOException e) {
      logger.debug("{} is not a URL; may be relative. Testing using parent URL {}", uri, _baseURL);
      XRLog.load(uri + " is not a URL; may be relative. Testing using parent URL " + _baseURL);
      try {
        URL result = new URL(new URL(_baseURL), uri);
        ret = result.toString();
      }
      catch (MalformedURLException e1) {
        logger.error("The default NaiveUserAgent cannot resolve the URL {} with base URL {}", uri, _baseURL);
        XRLog.exception("The default NaiveUserAgent cannot resolve the URL " + uri + " with base URL " + _baseURL);
      }
    }
    return ret;
  }

  /**
   * Returns the current baseUrl for this class.
   */
  @Override public String getBaseURL() {
    return _baseURL;
  }

  @Override public void documentStarted() {
    shrinkImageCache();
  }

  @Override public void documentLoaded() { /* ignore*/ }

  @Override public void onLayoutException(Throwable t) { /* ignore*/ }

  @Override public void onRenderException(Throwable t) { /* ignore*/ }
}
