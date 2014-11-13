package io.reactivex.lab.gateway.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.reactivex.netty.channel.RxDefaultThreadFactory;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientBuilder;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerBuilder;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

public class RxNettySSE {

    private static final EventLoopGroup EVENT_LOOP = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new RxNettyThreadFactory());

    public static HttpServer<ByteBuf, ServerSentEvent> createHttpServer(int port,
            RequestHandler<ByteBuf, ServerSentEvent> requestHandler) {
        return new HttpServerBuilder<>(port, requestHandler)
                .pipelineConfigurator(PipelineConfigurators.<ByteBuf> serveSseConfigurator())
                .eventLoop(EVENT_LOOP)
                .build();
    }

    public static HttpClient<ByteBuf, ServerSentEvent> createHttpClient(String host, int port) {
        return new HttpClientBuilder<ByteBuf, ServerSentEvent>(host, port)
                .pipelineConfigurator(PipelineConfigurators.<ByteBuf> clientSseConfigurator())
                .eventloop(EVENT_LOOP)
                .build();

    }

    public static class RxNettyThreadFactory extends RxDefaultThreadFactory {

        public RxNettyThreadFactory() {
            super("rx-netty-selector");
        }
    }

}
