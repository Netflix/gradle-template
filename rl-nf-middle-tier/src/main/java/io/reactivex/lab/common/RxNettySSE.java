package io.reactivex.lab.common;

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
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

public class RxNettySSE {

    private static final EventLoopGroup group = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(), new RxNettyThreadFactory());

    public static HttpServer<ByteBuf, ServerSentEvent> createHttpServer(int port,
            RequestHandler<ByteBuf, ServerSentEvent> requestHandler) {
        return new HttpServerBuilder<ByteBuf, ServerSentEvent>(port, requestHandler)
                .pipelineConfigurator(PipelineConfigurators.<ByteBuf> sseServerConfigurator())
                .eventLoop(group)
                .build();
    }

    public static HttpClient<ByteBuf, ServerSentEvent> createHttpClient(String host, int port) {
        return new HttpClientBuilder<ByteBuf, ServerSentEvent>(host, port)
                .pipelineConfigurator(PipelineConfigurators.<ByteBuf> sseClientConfigurator())
                .eventloop(group)
                .build();

    }

    public static class RxNettyThreadFactory extends RxDefaultThreadFactory {

        public RxNettyThreadFactory() {
            super("rx-netty-selector");
        }
    }

}
