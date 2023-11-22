package play.server.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.stream.ChunkedInput;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Invocation;
import play.InvocationContext;
import play.Invoker;
import play.Play;
import play.data.binding.CachedBoundActionMethodArgs;
import play.db.jpa.JPA;
import play.exceptions.UnexpectedException;
import play.i18n.Messages;
import play.libs.MimeTypes;
import play.mvc.ActionInvoker;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.mvc.Router;
import play.mvc.Scope;
import play.mvc.Scope.RenderArgs;
import play.mvc.results.NotFound;
import play.mvc.results.RenderStatic;
import play.server.IpParser;
import play.server.ServerAddress;
import play.server.ServerHelper;
import play.templates.JavaExtensions;
import play.utils.Utils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.COOKIE;
import static io.netty.handler.codec.http.HttpHeaders.Names.DATE;
import static io.netty.handler.codec.http.HttpHeaders.Names.ETAG;
import static io.netty.handler.codec.http.HttpHeaders.Names.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaders.Names.HOST;
import static io.netty.handler.codec.http.HttpHeaders.Names.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpHeaders.Names.IF_NONE_MATCH;
import static io.netty.handler.codec.http.HttpHeaders.Names.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpHeaders.Names.SET_COOKIE;
import static io.netty.handler.codec.http.HttpHeaders.Names.WARNING;
import static io.netty.handler.codec.http.HttpMethod.HEAD;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.defaultString;

@ParametersAreNonnullByDefault
public class PlayHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger logger = LoggerFactory.getLogger(PlayHandler.class);
    private final IpParser ipParser = new IpParser();
    private final ServerHelper serverHelper = new ServerHelper();
    private final FileService fileService = new FileService();
    private final Map<String, RenderStatic> staticPathsCache = new HashMap<>();

    /**
     * The Pipeline is given for a PlayHandler
     */
    final Map<String, ChannelHandler> pipelines = new HashMap<>();

    private final Invoker invoker;
    private final ActionInvoker actionInvoker;

    PlayHandler(Invoker invoker, ActionInvoker actionInvoker) {
        this.invoker = invoker;
        this.actionInvoker = actionInvoker;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final FullHttpRequest nettyRequest) {
        logger.trace("channelRead: begin");

        Request request = new Request();
        Response response = new Response();

        // Plain old HttpRequest
        try {
            Request.setCurrent(request);
            Response.setCurrent(response);

            request = parseRequest(ctx, nettyRequest);
            Request.setCurrent(request);

            // Buffered in memory output
            response.out = new ByteArrayOutputStream();

            // Direct output (will be set later)
            response.direct = null;

            // Streamed output (using response.writeChunk)
            final Request r = request;
            response.onWriteChunk(result -> writeChunk(r, response, ctx, nettyRequest, result));

            // Raw invocation
            boolean raw = Play.pluginCollection.rawInvocation(request, response, null, RenderArgs.current(), null);
            if (raw) {
                copyResponse(ctx, request, response, nettyRequest);
            } else {
                // Delegate to Play framework
                invoker.invoke(new Netty4Invocation(request, response, ctx, nettyRequest.retain()));
            }
        } catch (URISyntaxException ex) {
            // Do not log the stack trace for URI parsing errors. An info line suffices.
            logger.info("{} - {}", ex.getClass().getSimpleName(), ex.getMessage());
            serve400(ex, ctx);
        } catch (IllegalArgumentException ex) {
            logger.warn("Exception on request. serving 400 back", ex);
            serve400(ex, ctx);
        } catch (Exception ex) {
            logger.warn("Exception on request. serving 500 back", ex);
            serve500(ex, ctx, request, response);
        }

        logger.trace("channelRead: end");
    }

    private class Netty4Invocation extends Invocation {
        private final ChannelHandlerContext ctx;
        private final Request request;
        private final Response response;
        private final FullHttpRequest nettyRequest;

        public Netty4Invocation(Request request, Response response, ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
            this.ctx = ctx;
            this.request = request;
            this.response = response;
            this.nettyRequest = nettyRequest;
        }

        @Override
        public boolean init() throws IOException {
            logger.trace("init: begin");

            Request.setCurrent(request);
            Response.setCurrent(response);
            RenderArgs.current.set(null);
            CachedBoundActionMethodArgs.init();

            try {
                if (Play.mode == Play.Mode.DEV) {
                    Router.detectChanges();
                }
                if (Play.mode == Play.Mode.PROD
                  && staticPathsCache.containsKey(request.domain + " " + request.method + " " + request.path)) {
                    RenderStatic rs;
                    synchronized (staticPathsCache) {
                        rs = staticPathsCache.get(request.domain + " " + request.method + " " + request.path);
                    }
                    serveStatic(rs, ctx, request, response, nettyRequest);
                    logger.trace("init: end false");
                    return false;
                }
                Router.instance.routeOnlyStatic(request);
                super.init();
            } catch (NotFound nf) {
                serve404(nf, ctx, request);
                logger.trace("init: end false");
                return false;
            } catch (RenderStatic rs) {
                if (Play.mode == Play.Mode.PROD) {
                    synchronized (staticPathsCache) {
                        staticPathsCache.put(request.domain + " " + request.method + " " + request.path, rs);
                    }
                }
                serveStatic(rs, ctx, request, response, nettyRequest);
                logger.trace("init: end false");
                return false;
            }

            logger.trace("init: end true");
            return true;
        }

        @Override
        public InvocationContext getInvocationContext() {
            ActionInvoker.resolve(request);
            return new InvocationContext(Http.invocationType, request.invokedMethod.getAnnotations(),
              request.invokedMethod.getDeclaringClass().getAnnotations());
        }

        @Override
        public void run() {
            try {
                logger.trace("run: begin");
                onStarted();
                try {
                    preInit();
                    if (init()) {
                        before();
                        JPA.withinFilter(() -> {
                            execute();
                            return null;
                        });
                        after();
                        onSuccess();
                    }
                } catch (Throwable e) {
                    onActionInvocationException(request, response, e);
                } finally {
                    Play.pluginCollection.onActionInvocationFinally(request, response);
                    InvocationContext.current.remove();
                }
            } catch (Exception e) {
                serve500(e, ctx, request, response);
            } finally {
                nettyRequest.release();
            }
            logger.trace("run: end");
        }

        @Override
        public void execute() {
            logger.trace("execute: begin");
            if (!ctx.channel().isActive()) {
                logger.trace("execute: end, ignored");
                return;
            }

            // Check the exceeded size before re rendering so we can render the
            // error if the size is exceeded
            saveExceededSizeError(nettyRequest, request);
            actionInvoker.invoke(request, response);
            logger.trace("execute: end");
        }

        @Override
        public void onSuccess() throws Exception {
            super.onSuccess();
            logger.trace("onSuccess: begin");
            if (response.chunked) {
                closeChunked(response);
            } else {
                copyResponse(ctx, request, response, nettyRequest);
            }
            logger.trace("onSuccess: end");
        }
    }

    private void saveExceededSizeError(FullHttpRequest nettyRequest, Request request) {
        String warning = nettyRequest.headers().get(WARNING);
        String length = nettyRequest.headers().get(CONTENT_LENGTH);
        if (warning != null) {
            logger.trace("saveExceededSizeError: begin");

            try {
                StringBuilder error = new StringBuilder();
                error.append("\u0000");
                // Cannot put warning which is
                // play.netty.content.length.exceeded
                // as Key as it will result error when printing error
                error.append("play.netty.maxContentLength");
                error.append(":");
                String size;
                try {
                    size = JavaExtensions.formatSize(Long.parseLong(length));
                } catch (Exception e) {
                    size = length + " bytes";
                }
                error.append(Messages.get(warning, size));
                error.append("\u0001");
                error.append(size);
                error.append("\u0000");
                if (request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS") != null
                  && request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS").value != null) {
                    error.append(request.cookies.get(Scope.COOKIE_PREFIX + "_ERRORS").value);
                }
                String errorData = URLEncoder.encode(error.toString(), UTF_8);
                Http.Cookie cookie = new Http.Cookie(Scope.COOKIE_PREFIX + "_ERRORS", errorData);
                request.cookies.put(Scope.COOKIE_PREFIX + "_ERRORS", cookie);
                logger.trace("saveExceededSizeError: end");
            } catch (Exception e) {
                throw new UnexpectedException("Error serialization problem", e);
            }
        }
    }

    private void addToResponse(Response response, HttpResponse nettyResponse) {
        addContentTypeToResponse(nettyResponse, response);
        addHeadersToResponse(nettyResponse, response.headers);
        addDateToResponse(nettyResponse);
        addCookiesToResponse(nettyResponse, response.cookies);
        addCacheControlToResponse(nettyResponse, response);
    }

    private void addContentTypeToResponse(HttpResponse nettyResponse, Response response) {
        String contentType = serverHelper.getContentTypeValue(response);
        nettyResponse.headers().set(CONTENT_TYPE, contentType);
    }

    private void addHeadersToResponse(HttpResponse nettyResponse, Map<String, Http.Header> headers) {
        for (Map.Entry<String, Http.Header> entry : headers.entrySet()) {
            Http.Header hd = entry.getValue();
            for (String value : hd.values) {
                nettyResponse.headers().add(entry.getKey(), value);
            }
        }
    }

    private void addDateToResponse(HttpResponse nettyResponse) {
        nettyResponse.headers().set(DATE, Utils.getHttpDateFormatter().format(new Date()));
    }

    private void addCookiesToResponse(HttpResponse nettyResponse, Map<String, Http.Cookie> cookies) {
        for (Http.Cookie cookie : cookies.values()) {
            Cookie c = new DefaultCookie(cookie.name, cookie.value);
            c.setSecure(cookie.secure);
            c.setPath(cookie.path);
            if (cookie.domain != null) {
                c.setDomain(cookie.domain);
            }
            if (cookie.maxAge != null) {
                c.setMaxAge(cookie.maxAge);
            }
            c.setHttpOnly(cookie.httpOnly);
            nettyResponse.headers().add(SET_COOKIE, ServerCookieEncoder.STRICT.encode(c));
        }
    }

    private void addCacheControlToResponse(HttpResponse nettyResponse, Response response) {
        if (!response.headers.containsKey(CACHE_CONTROL) && !response.headers.containsKey(EXPIRES) && !(response.direct instanceof File)) {
            nettyResponse.headers().set(CACHE_CONTROL, "no-cache");
        }
    }

    private void writeResponse(ChannelHandlerContext ctx, Response response, FullHttpResponse nettyResponse,
                               HttpRequest nettyRequest) {
        logger.trace("writeResponse: begin");

        boolean keepAlive = isKeepAlive(nettyRequest);
        byte[] content = nettyRequest.method().equals(HEAD) ? new byte[0] : response.out.toByteArray();

        ByteBuf buf = Unpooled.copiedBuffer(content);
        nettyResponse = nettyResponse.replace(buf);

        if (!nettyResponse.status().equals(NOT_MODIFIED)) {
            if (logger.isTraceEnabled()) {
                logger.trace("writeResponse: content length [{}]", response.out.size());
            }
            setContentLength(nettyResponse, response.out.size());
        }

        ChannelFuture f = null;
        if (ctx.channel().isOpen()) {
            f = ctx.channel().writeAndFlush(nettyResponse);
        } else {
            logger.debug("Try to write on a closed channel[keepAlive:{}]: Remote host may have closed the connection", keepAlive);
        }

        // Decide whether to close the connection or not.
        if (f != null && !keepAlive) {
            // Close the connection when the whole content is written out.
            f.addListener(ChannelFutureListener.CLOSE);
        }
        logger.trace("writeResponse: end");
    }

    private void copyResponse(ChannelHandlerContext ctx, Request request, Response response, FullHttpRequest nettyRequest) throws Exception {
        logger.trace("copyResponse: begin");

        FullHttpResponse nettyResponse = createHttpResponse(HttpResponseStatus.valueOf(response.status));

        addToResponse(response, nettyResponse);

        File file = response.direct instanceof File ? (File) response.direct : null;
        InputStream is = response.direct instanceof InputStream ? (InputStream) response.direct : null;
        ChunkedInput stream = response.direct instanceof ChunkedInput ? (ChunkedInput) response.direct : null;

        boolean keepAlive = isKeepAlive(nettyRequest);
        if (file != null && file.isFile()) {
            addEtag(nettyRequest, nettyResponse, file);
            if (nettyResponse.status().equals(NOT_MODIFIED)) {

                Channel ch = ctx.channel();

                // Write the initial line and the header.
                ChannelFuture writeFuture = ch.writeAndFlush(nettyResponse);

                if (!keepAlive) {
                    // Close the connection when the whole content is
                    // written out.
                    writeFuture.addListener(ChannelFutureListener.CLOSE);
                }
            } else {
                fileService.serve(file, nettyRequest, nettyResponse, ctx, request, response, ctx.channel());
            }
        } else if (is != null) {
            ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
            if (!nettyRequest.method().equals(HEAD) && !nettyResponse.status().equals(NOT_MODIFIED)) {
                writeFuture = ctx.channel().writeAndFlush(new ChunkedStream(is));
            } else {
                is.close();
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else if (stream != null) {
            ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
            if (!nettyRequest.method().equals(HEAD) && !nettyResponse.status().equals(NOT_MODIFIED)) {
                writeFuture = ctx.channel().writeAndFlush(stream);
            } else {
                stream.close();
            }
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            writeResponse(ctx, response, nettyResponse, nettyRequest);
        }
        logger.trace("copyResponse: end");
    }

    private String getRemoteIPAddress(Channel ch) {
        return ipParser.getRemoteIpAddress((InetSocketAddress) ch.remoteAddress());
    }

    Request parseRequest(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) throws URISyntaxException {
        logger.trace("parseRequest: begin, URI = {}", nettyRequest.uri());

        String host = nettyRequest.headers().get(HOST);
        String contentType = nettyRequest.headers().get(CONTENT_TYPE);
        URI uri = new URI(nettyRequest.uri());
        String relativeUrl = serverHelper.relativeUrl(uri.getPath(), uri.getQuery());
        boolean isLoopback = ipParser.isLoopback(host, (InetSocketAddress) ctx.channel().remoteAddress());
        ServerAddress serverAddress = ipParser.parseHost(host);
        InputStream body = readBody(nettyRequest);

        Request request = Request.createRequest(
          getRemoteIPAddress(ctx.channel()),
          nettyRequest.method().name(),
          uri.getPath(), uri.getQuery(), contentType, body, relativeUrl, 
          serverAddress.host, isLoopback, serverAddress.port, serverAddress.domain,
            getHeaders(nettyRequest), getCookies(nettyRequest));

        logger.trace("parseRequest: end");
        return request;
    }

    @Nonnull
    @CheckReturnValue
    private static InputStream readBody(FullHttpRequest nettyRequest) {
        InputStream body;
        ByteBuf b = nettyRequest.content();
        //if (b instanceof FileChannelBuffer) {
        //    FileChannelBuffer buffer = (FileChannelBuffer) b;
        //    // An error occurred
        //    int max = serverHelper.maxContentLength();

        //    body = buffer.getInputStream();
        //    if (!(max == -1 || body.available() < max)) {
        //        body = new ByteArrayInputStream(new byte[0]);
        //    }

        //} else {
        body = new ByteBufInputStream(b);
        //}
        return body;
    }

    @Nonnull
    @CheckReturnValue
    private Map<String, Http.Header> getHeaders(FullHttpRequest nettyRequest) {
        Map<String, Http.Header> headers = new HashMap<>(16);

        for (String key : nettyRequest.headers().names()) {
            List<String> headerValues = unmodifiableList(nettyRequest.headers().getAll(key));
            Http.Header hd = new Http.Header(key.toLowerCase(), headerValues);
            headers.put(hd.name, hd);
        }

        return headers;
    }

    private Map<String, Http.Cookie> getCookies(FullHttpRequest nettyRequest) {
        Map<String, Http.Cookie> cookies = new HashMap<>(16);
        String value = nettyRequest.headers().get(COOKIE);
        if (value != null) {
            Set<Cookie> cookieSet = ServerCookieDecoder.STRICT.decode(value);
            for (Cookie cookie : cookieSet) {
                Http.Cookie playCookie = new Http.Cookie(cookie.name(), cookie.value());
                playCookie.path = cookie.path();
                playCookie.domain = cookie.domain();
                playCookie.secure = cookie.isSecure();
                playCookie.httpOnly = cookie.isHttpOnly();
                cookies.put(playCookie.name, playCookie);
            }
        }
        return cookies;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        try {
            // If we get a TooLongFrameException, we got a request exceeding 8k.
            // Log this, we can't call serve500()
            if (cause instanceof TooLongFrameException) {
                logger.error("Request exceeds 8192 bytes", cause);
            }
            ctx.channel().close();
        } catch (Exception ex) {
            logger.warn("Failed to close channel for {}", ctx.name(), ex);
        }
    }

    private void serve400(Exception e, ChannelHandlerContext ctx) {
        logger.trace("serve400: begin");
        FullHttpResponse nettyResponse = createHttpResponse(HttpResponseStatus.BAD_REQUEST);
        nettyResponse.headers().set(CONTENT_TYPE, "text/plain");
        printResponse(ctx, nettyResponse, e.getMessage() + '\n');
        logger.trace("serve400: end");
    }

    private void serve404(NotFound e, ChannelHandlerContext ctx, Request request) {
        logger.trace("serve404: begin");
        String format = defaultString(request.format, "txt");
        String contentType = MimeTypes.getContentType("404." + format, "text/plain");

        FullHttpResponse nettyResponse = createHttpResponse(HttpResponseStatus.NOT_FOUND);
        nettyResponse.headers().set(CONTENT_TYPE, contentType);

        String errorHtml = serverHelper.generateNotFoundResponse(request, format, e);
        printResponse(ctx, nettyResponse, errorHtml);
        logger.trace("serve404: end");
    }

    private void printResponse(ChannelHandlerContext ctx, FullHttpResponse nettyResponse, String errorHtml) {
        byte[] bytes = errorHtml.getBytes(Response.current().encoding);
        ByteBuf buf = ctx.alloc().buffer(bytes.length).writeBytes(bytes);
        setContentLength(nettyResponse, bytes.length);
        nettyResponse = nettyResponse.replace(buf);
        ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
        writeFuture.addListener(ChannelFutureListener.CLOSE);
    }

    private void serve500(Exception e, ChannelHandlerContext ctx, Request request, Response response) {
        logger.trace("serve500: begin");
        FullHttpResponse nettyResponse = createHttpResponse(INTERNAL_SERVER_ERROR);
        Charset encoding = response.encoding;

        try {
            flushCookies(response, nettyResponse);

            String format = requireNonNullElse(request.format, "txt");
            nettyResponse.headers().set("Content-Type", MimeTypes.getContentType("500." + format, "text/plain"));
            try {
                String errorHtml = serverHelper.generateErrorResponse(request, format, e);

                byte[] bytes = errorHtml.getBytes(encoding);
                ByteBuf buf = Unpooled.copiedBuffer(bytes);
                setContentLength(nettyResponse, bytes.length);
                nettyResponse = nettyResponse.replace(buf);
                ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
                writeFuture.addListener(ChannelFutureListener.CLOSE);
                logger.error("Internal Server Error (500) for {} {} ({})", request.method, request.url, e.getClass().getSimpleName(), e);
            } catch (Throwable ex) {
                logger.error("Internal Server Error (500) for {} {} ({})", request.method, request.url, e.getClass().getSimpleName(), e);
                logger.error("Error during the 500 response generation", ex);
                byte[] bytes = "Internal Error".getBytes(encoding);
                ByteBuf buf = Unpooled.copiedBuffer(bytes);
                setContentLength(nettyResponse, bytes.length);
                nettyResponse = nettyResponse.replace(buf);
                ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } catch (RuntimeException exxx) {
            try {
                byte[] bytes = "Internal Error".getBytes(encoding);
                ByteBuf buf = Unpooled.copiedBuffer(bytes);
                setContentLength(nettyResponse, bytes.length);
                nettyResponse = nettyResponse.replace(buf);
                ChannelFuture writeFuture = ctx.channel().writeAndFlush(nettyResponse);
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            } catch (Exception fex) {
                logger.error("(encoding ?)", fex);
            }
            throw exxx;
        }
        logger.trace("serve500: end");
    }

    private void flushCookies(Response response, FullHttpResponse nettyResponse) {
        try {
            Map<String, Http.Cookie> cookies = response.cookies;
            addCookiesToResponse(nettyResponse, cookies);

        } catch (Exception e) {
            logger.error("Failed to flush cookies", e);
        }
    }

    private FullHttpResponse createHttpResponse(HttpResponseStatus status) {
        return new DefaultFullHttpResponse(HTTP_1_1, status);
    }

    private HttpResponse createByteHttpResponse(HttpResponseStatus status) {
        return new DefaultHttpResponse(HTTP_1_1, status);
    }

    private void serveStatic(RenderStatic renderStatic, ChannelHandlerContext ctx,
                             Request playRequest, Response playResponse,
                             FullHttpRequest nettyRequest) {
        logger.trace("serveStatic: begin");

        HttpResponse nettyResponse = createByteHttpResponse(HttpResponseStatus.valueOf(playResponse.status));
        try {
            File file = serverHelper.findFile(renderStatic.file);

            if ((file == null || !file.exists())) {
                serve404(new NotFound("The file " + renderStatic.file + " does not exist"), ctx, playRequest);
            } else {
                serveLocalFile(file, playRequest, playResponse, ctx, nettyRequest, nettyResponse);
            }
        } catch (Throwable ez) {
            logger.error("serveStatic for request {} {}", playRequest.method, playRequest.url, ez);
            try {
                FullHttpResponse errorResponse = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
                byte[] bytes = "Internal Error".getBytes(playResponse.encoding);
                ByteBuf buf = Unpooled.copiedBuffer(bytes);
                setContentLength(nettyResponse, bytes.length);
                errorResponse = errorResponse.replace(buf);
                ChannelFuture future = ctx.channel().writeAndFlush(errorResponse);
                future.addListener(ChannelFutureListener.CLOSE);
            } catch (Exception ex) {
                logger.error("serveStatic for request {} {}", playRequest.method, playRequest.url, ex);
            }
        }
        logger.trace("serveStatic: end");
    }

    private void serveLocalFile(File localFile, Request playRequest, Response playResponse,
                                ChannelHandlerContext ctx, 
                                FullHttpRequest nettyRequest, HttpResponse nettyResponse) throws FileNotFoundException {
        boolean keepAlive = isKeepAlive(nettyRequest);
        addEtag(nettyRequest, nettyResponse, localFile);
        Channel ch = ctx.channel();
        if (nettyResponse.status().equals(NOT_MODIFIED)) {
            ChannelFuture writeFuture = ch.writeAndFlush(nettyResponse);
            if (!keepAlive) {
                writeFuture.addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            fileService.serve(localFile, nettyRequest, nettyResponse, ctx, playRequest, playResponse, ch);
        }
    }

    private boolean isModified(String etag, long last, FullHttpRequest nettyRequest) {
        String ifNoneMatch = nettyRequest.headers().get(IF_NONE_MATCH);
        String ifModifiedSince = nettyRequest.headers().get(IF_MODIFIED_SINCE);
        return serverHelper.isModified(etag, last, ifNoneMatch, ifModifiedSince);
    }

    private <T extends HttpResponse> void addEtag(FullHttpRequest nettyRequest, T httpResponse, File file) {
        if (Play.mode == Play.Mode.DEV) {
            httpResponse.headers().set(CACHE_CONTROL, "no-cache");
        } else {
            // Check if Cache-Control header is not set
            if (httpResponse.headers().get(CACHE_CONTROL) == null) {
                String maxAge = Play.configuration.getProperty("http.cacheControl", "3600");
                if ("0".equals(maxAge)) {
                    httpResponse.headers().set(CACHE_CONTROL, "no-cache");
                } else {
                    httpResponse.headers().set(CACHE_CONTROL, "max-age=" + maxAge);
                }
            }
        }
        boolean useEtag = "true".equals(Play.configuration.getProperty("http.useETag", "true"));
        long last = file.lastModified();
        String etag = "\"" + last + "-" + file.hashCode() + "\"";
        if (!isModified(etag, last, nettyRequest)) {
            if (nettyRequest.method().equals(HttpMethod.GET)) {
                httpResponse.setStatus(NOT_MODIFIED);
            }
            if (useEtag) {
                httpResponse.headers().set(ETAG, etag);
            }

        } else {
            httpResponse.headers().set(LAST_MODIFIED, Utils.getHttpDateFormatter().format(new Date(last)));
            if (useEtag) {
                httpResponse.headers().set(ETAG, etag);
            }
        }
    }

    private boolean isKeepAlive(HttpMessage message) {
        return HttpUtil.isKeepAlive(message) && message.protocolVersion().equals(HTTP_1_1);
    }

    private void setContentLength(HttpMessage message, long contentLength) {
        message.headers().set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(contentLength));
    }

    private void writeChunk(Request playRequest, Response playResponse, ChannelHandlerContext ctx, FullHttpRequest nettyRequest, Object chunk) {
        try {
            if (playResponse.direct == null) {
                playResponse.setHeader("Transfer-Encoding", "chunked");
                playResponse.direct = new LazyChunkedInput();
                copyResponse(ctx, playRequest, playResponse, nettyRequest);
            }
            ((LazyChunkedInput) playResponse.direct).writeChunk(chunk, playResponse.encoding);

            if (this.pipelines.get("ChunkedWriteHandler") != null) {
                ((ChunkedWriteHandler) this.pipelines.get("ChunkedWriteHandler")).resumeTransfer();
            }
            if (this.pipelines.get("SslChunkedWriteHandler") != null) {
                ((ChunkedWriteHandler) this.pipelines.get("SslChunkedWriteHandler")).resumeTransfer();
            }
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private void closeChunked(Response playResponse) {
        ((LazyChunkedInput) playResponse.direct).close();
        if (this.pipelines.get("ChunkedWriteHandler") != null) {
            ((ChunkedWriteHandler) this.pipelines.get("ChunkedWriteHandler")).resumeTransfer();
        }
        if (this.pipelines.get("SslChunkedWriteHandler") != null) {
            ((ChunkedWriteHandler) this.pipelines.get("SslChunkedWriteHandler")).resumeTransfer();
        }
    }
}
