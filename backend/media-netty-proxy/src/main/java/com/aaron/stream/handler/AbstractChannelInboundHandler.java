package com.aaron.stream.handler;

import com.aaron.stream.config.ProxyProperties;
import com.aaron.stream.utils.HikRest;
import com.aaron.stream.utils.ProxyInit;
import com.aaron.stream.vo.ProxyUrlInfo;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractChannelInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    protected final static ProxyProperties PROXY_PROPERTIES = ProxyInit.getProxyProperties();
    protected final static String URL_REGEX = "(.*?):(\\d+)/(.+)";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        // Processing OPTIONS request
        HttpMethod method = msg.method();
        if (method.equals(HttpMethod.OPTIONS)) {
            sendOkResponse(ctx);
            return;
        }

        // Basic Authorization
        String authHeader = msg.headers().get(HttpHeaderNames.AUTHORIZATION);
        if (!isValidAuth(authHeader)) {
            sendUnauthorizedResponse(ctx);
            return;
        }

        String redirectUrl = fetchRedirectUrl(msg);
        doChannelRead0(ctx, msg, redirectUrl);
    }

    protected abstract void doChannelRead0(ChannelHandlerContext ctx, FullHttpRequest msg, String streamUrl) throws Exception;

    @Override
    public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) {
        cause.printStackTrace();
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Basic Valid
     *
     * @param authHeader header
     * @return true or false
     */
    private boolean isValidAuth(String authHeader) {
        String username = PROXY_PROPERTIES.getUsername();
        String password = PROXY_PROPERTIES.getPassword();
        String expectedAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        return expectedAuth.equals(authHeader);
    }

    /**
     * Get stream url
     *
     * @param msg Request
     * @return stream url
     * @throws Exception exception
     */
    @NotNull
    protected String fetchRedirectUrl(@NotNull FullHttpRequest msg) throws Exception {
        if (PROXY_PROPERTIES.isTest()) {
           return "rtsp://admin:123456@127.0.0.1:554";
        }
        String hostHeader = msg.headers().get(HttpHeaderNames.HOST);
        String fulUri = hostHeader + msg.uri();
        ProxyUrlInfo info = parseCameraCodeByRequestUrl(fulUri);
        return HikRest.getRtspUrl(info.getId());
    }

    /**
     * Parse camera code
     *
     * @param url request url
     * @return Proxy request info {@link ProxyUrlInfo}
     */
    @NotNull
    protected ProxyUrlInfo parseCameraCodeByRequestUrl(String url) {
        ProxyUrlInfo info = new ProxyUrlInfo();
        Pattern pattern = Pattern.compile(URL_REGEX);
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) throw new RuntimeException("Invalid request url: " + url);
        info.setHost(matcher.group(1));
        info.setPort(Integer.parseInt(matcher.group(2)));
        info.setId(matcher.group(3));
        return info;
    }

    /**
     * Send 200
     *
     * @param ctx Channel context
     */
    protected void sendOkResponse(@NotNull ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Send 401 error
     *
     * @param ctx Channel context
     */
    protected void sendUnauthorizedResponse(@NotNull ChannelHandlerContext ctx) {
        // Authentication failed
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED
        );
        response.headers().set(HttpHeaderNames.WWW_AUTHENTICATE, "Basic realm=\"Restricted\"");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Send 415 error
     *
     * @param ctx Channel context
     */
    protected void sendUnSupportResponse(@NotNull ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.UNSUPPORTED_MEDIA_TYPE
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Send 500 error
     *
     * @param ctx Channel context
     */
    protected void sendErrorResponse(@NotNull ChannelHandlerContext ctx) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR
        );
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
