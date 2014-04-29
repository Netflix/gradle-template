package io.reactivex.lab.edge;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.lab.edge.routes.RouteForDeviceHome;
import io.reactivex.lab.edge.routes.mock.TestRouteBasic;
import io.reactivex.lab.edge.routes.mock.TestRouteWithHystrix;
import io.reactivex.lab.edge.routes.mock.TestRouteWithSimpleFaultTolerance;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;
import io.reactivex.netty.serialization.ContentTransformer;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

public class EdgeServer {

    public static void main(String... args) {
        // hystrix stream => http://localhost:9999
        startHystrixMetricsStream();

        System.out.println("Server => Starting at http://localhost:8080/");
        System.out.println("   Sample URLs: ");
        System.out.println("      - /device/home");
        System.out.println("      - /helloworld.js");
        System.out.println("      - /hello.js?name=Dave");
        System.out.println("      - /async.js");
        System.out.println("----------------------------------------------------------------");

        // start web services => http://localhost:8080
        RxNetty.createHttpServer(8080, (request, response) -> {
            return Observable.defer(() -> {
                System.out.println("Server => Request: " + request.getPath());
                HystrixRequestContext.initializeContext();
                try {
                    return handleRoutes(request, response);
                } catch (Throwable e) {
                    System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    return response.writeStringAndFlush("Error 500: Bad Request\n" + e.getMessage() + "\n");
                }
            }).onErrorResumeNext(error -> {
                System.err.println("Server => Error: " + error.getMessage());
                error.printStackTrace();
                return writeError(request, response, "Failed: " + error.getMessage());
            }).doOnTerminate(() -> {
                if (HystrixRequestContext.isCurrentThreadInitialized()) {
                    System.out.println("Server => Hystrix Log [" + request.getPath() + "] => " + HystrixRequestLog.getCurrentRequest().getExecutedCommandsAsString());
                    HystrixRequestContext.getContextForCurrentThread().shutdown();
                } else {
                    System.err.println("HystrixRequestContext not initialized for thread: " + Thread.currentThread());
                }
            });
        }).startAndWait();
    }

    /**
     * Hard-coded route handling.
     */
    private static Observable<Void> handleRoutes(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        if (request.getPath().equals("/device/home")) {
            return RouteForDeviceHome.getInstance().handle(request, response);
        } else if (request.getPath().equals("/testBasic")) {
            return TestRouteBasic.handle(request, response);
        } else if (request.getPath().equals("/testWithSimpleFaultTolerance")) {
            return TestRouteWithSimpleFaultTolerance.handle(request, response);
        } else if (request.getPath().equals("/testWithHystrix")) {
            return TestRouteWithHystrix.handle(request, response);
        } else if (request.getPath().endsWith(".js")) {
            System.out.println("Server => Javascript Request: " + request.getPath());
            return JavascriptRuntime.getInstance().handleRequest(request, response);
        } else {
            return writeError(request, response, "Unknown path: " + request.getPath());
        }
    }

    private static void startHystrixMetricsStream() {
        RxNetty.createHttpServer(9999, (request, response) -> {
            System.out.println("Server => Start Hystrix Stream at http://localhost:9999");
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
        }, PipelineConfigurators.<ByteBuf> sseServerConfigurator()).start();
    }

    final static Observable<String> streamPoller = Observable.create((Subscriber<? super String> s) -> {
        try {
            System.out.println("Server => Start Hystrix Metric Poller");
            HystrixMetricsPoller poller = new HystrixMetricsPoller(json -> {
                s.onNext(json);
            }, 1000);
            s.add(Subscriptions.create(() -> {
                System.out.println("Server => Shutdown Hystrix Stream");
                poller.shutdown();
            }));
            poller.start();
        } catch (Exception e) {
            s.onError(e);
        }
    }).publish().refCount();

    public static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<ByteBuf> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message);
    }

    public static SSETransformer SSE_TRANSFORMER = new SSETransformer();

    private static class SSETransformer implements ContentTransformer<ServerSentEvent> {
        @Override
        public ByteBuf transform(ServerSentEvent toTransform, ByteBufAllocator byteBufAllocator) {
            StringBuilder eventBuilder = new StringBuilder();
            eventBuilder.append(toTransform.getEventName());
            eventBuilder.append(": ");
            eventBuilder.append(toTransform.getEventData());
            eventBuilder.append("\n\n");
            String data = eventBuilder.toString();
            return byteBufAllocator.buffer(data.length()).writeBytes(data.getBytes());
        }
    }
}
