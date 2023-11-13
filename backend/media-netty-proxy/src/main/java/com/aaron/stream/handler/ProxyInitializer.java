package com.aaron.stream.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class ProxyInitializer extends ChannelInitializer<Channel> {
    private static final int MAX_CONTENT_LENGTH = 65536;

    @Override
    protected void initChannel(Channel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new CorsHandler(corsConfig()));
        pipeline.addLast(new VideoStreamHandler());
    }

    private CorsConfig corsConfig() {
        return CorsConfigBuilder.forAnyOrigin()
                .allowedRequestMethods(
                        HttpMethod.GET,
                        HttpMethod.POST,
                        HttpMethod.PUT,
                        HttpMethod.DELETE,
                        HttpMethod.OPTIONS)
                .allowedRequestHeaders(
                        HttpHeaderNames.ORIGIN,
                        HttpHeaderNames.X_REQUESTED_WITH,
                        HttpHeaderNames.CONTENT_TYPE,
                        HttpHeaderNames.ACCEPT,
                        HttpHeaderNames.AUTHORIZATION)
                .allowNullOrigin()
                .allowCredentials()
                .maxAge(3600) // Cache time for preflight requests, in seconds
                .build();
    }
}
