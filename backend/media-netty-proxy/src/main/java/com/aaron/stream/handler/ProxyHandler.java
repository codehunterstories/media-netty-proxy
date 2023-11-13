package com.aaron.stream.handler;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import org.jetbrains.annotations.NotNull;

/**
 * Processing Proxy
 */
public class ProxyHandler extends AbstractChannelInboundHandler {

    @Override
    protected void doChannelRead0(ChannelHandlerContext ctx, @NotNull FullHttpRequest msg, String streamUrl) {
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, streamUrl);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

}
