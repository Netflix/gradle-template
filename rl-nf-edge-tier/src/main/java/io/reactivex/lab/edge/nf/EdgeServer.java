package io.reactivex.lab.edge.nf;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.lab.edge.common.RxNettySSE;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;

public class EdgeServer {

    public static void main(String... args) {
        // hystrix stream => http://localhost:9999
        startHystrixMetricsStream();

        // start web services => http://localhost:8080
        RxNettySSE.createHttpServer(8080, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            try {
                if (request.getPath().equals("/device/home")) {
                    return EndpointForDeviceHome.getInstance().handle(request, response).onErrorResumeNext(error -> {
                        error.printStackTrace();
                        return writeError(request, response, "Failed: " + error.getMessage());
                    });
                } else if (request.getPath().endsWith(".js")) {
                    System.out.println("Server => Javascript Request: " + request.getPath());
                    return JavascriptRuntime.getInstance().handleRequest(request, response);
                } else {
                    return writeError(request, response, "Unknown path: " + request.getPath());
                }
            } catch (Throwable e) {
                System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                return response.writeStringAndFlush("Error 500: Bad Request\n" + e.getMessage() + "\n");
            }
        }).startAndWait();
    }

    private static void startHystrixMetricsStream() {
        RxNettySSE.createHttpServer(9999, (request, response) -> {
            System.out.println("Start Hystrix Stream");
            response.getHeaders().add("content-type", "text/event-stream");
            return Observable.create((Subscriber<? super Void> s) -> {
                s.add(streamPoller.subscribe(json -> {
                    response.writeAndFlush(new ServerSentEvent("", "data", json));
                }, error -> {
                    s.onError(error);
                }));

                s.add(Observable.interval(1000, TimeUnit.MILLISECONDS).flatMap(n -> {
                    return response.writeAndFlush(new ServerSentEvent("", "ping", ""))
                            .onErrorReturn(e -> {
                                System.out.println("Connection closed, unsubscribing from Hystrix Stream");
                                s.unsubscribe();
                                return null;
                            });
                }).subscribe());
            });
        }).start();
    }

    final static Observable<String> streamPoller = Observable.create((Subscriber<? super String> s) -> {
        try {
            System.out.println("Start Hystrix Metric Poller");
            HystrixMetricsPoller poller = new HystrixMetricsPoller(json -> {
                s.onNext(json);
            }, 1000);
            s.add(Subscriptions.create(() -> {
                System.out.println("Shutdown Hystrix Stream");
                poller.shutdown();
            }));
            poller.start();
        } catch (Exception e) {
            s.onError(e);
        }
    }).publish().refCount();

    public static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<?> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message + "\n");
    }
}
