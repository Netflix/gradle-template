package io.reactivex.lab.tutorial;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;

import java.util.concurrent.TimeUnit;

public class ClientServer {

    public static HttpServer<ByteBuf, ServerSentEvent> startServer(int port, int interval, TimeUnit intervalUnit) {
        HttpServer<ByteBuf, ServerSentEvent> server = RxNetty.createHttpServer(port,
                             (request, response) -> Observable.interval(interval, intervalUnit)
                             .flatMap(tick -> response.writeStringAndFlush("data: Interval: " + tick + "\n")),
                             PipelineConfigurators.serveSseConfigurator());
        return server.start();
    }

    public static HttpServer<ByteBuf, ServerSentEvent> startServer(int port) {
        return startServer(port, 1, TimeUnit.SECONDS);
    }

    public static Observable<ServerSentEvent> createRequest(int port) {
        return RxNetty.<ByteBuf, ServerSentEvent>createHttpClient("127.0.0.1", port,
                                                           PipelineConfigurators.clientSseConfigurator())
               .submit(HttpClientRequest.createGet("/"))
               .flatMap(response -> response.getContent());
    }

    public static void main(String[] args) {
        HttpServer<ByteBuf, ServerSentEvent> server = startServer(8088);
        createRequest(server.getServerPort()).toBlocking()
                                             .forEach(sse -> System.out.println(sse.contentAsString()));
    }
}
