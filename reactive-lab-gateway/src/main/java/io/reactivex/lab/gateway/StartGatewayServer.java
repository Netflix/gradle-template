package io.reactivex.lab.gateway;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.lab.gateway.loadbalancer.DiscoveryAndLoadBalancer;
import io.reactivex.lab.gateway.loadbalancer.LoadBalancerFactory;
import io.reactivex.lab.gateway.routes.RouteForDeviceHome;
import io.reactivex.lab.gateway.routes.mock.TestRouteBasic;
import io.reactivex.lab.gateway.routes.mock.TestRouteWithHystrix;
import io.reactivex.lab.gateway.routes.mock.TestRouteWithSimpleFaultTolerance;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;

import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

public class StartGatewayServer {

    static {
        // discovery config
        System.setProperty("reactivelab.eureka.server.host", "127.0.0.1");
        System.setProperty("reactivelab.eureka.server.read.port", "7001");
        System.setProperty("reactivelab.eureka.server.write.port", "7002");
    }

    public static void main(String... args) {
        // initialize
        DiscoveryAndLoadBalancer.getFactory();
        // hystrix stream => http://localhost:9999
        startHystrixMetricsStream();

        System.out.println("Server => Starting at http://localhost:8080/");
        System.out.println("   Sample URLs: ");
        System.out.println("      - http://localhost:8080/device/home?userId=123");
        System.out.println("----------------------------------------------------------------");

        // start web services => http://localhost:8080
        RxNetty.createHttpServer(8080, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            return Observable.defer(() -> {
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
                response.close();
            });
        }).startAndWait();
    }

    /**
     * Hard-coded route handling.
     */
    private static Observable<Void> handleRoutes(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        if (request.getPath().equals("/device/home")) {
            return new RouteForDeviceHome().handle(request, response);
        } else if (request.getPath().equals("/testBasic")) {
            return TestRouteBasic.handle(request, response);
        } else if (request.getPath().equals("/testWithSimpleFaultTolerance")) {
            return TestRouteWithSimpleFaultTolerance.handle(request, response);
        } else if (request.getPath().equals("/testWithHystrix")) {
            return TestRouteWithHystrix.handle(request, response);
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
                    response.writeStringAndFlush("data: " + json);
                }, error -> {
                    s.onError(error);
                }));

                s.add(Observable.interval(1000, TimeUnit.MILLISECONDS).flatMap(n -> {
                    return response.writeStringAndFlush("ping:")
                            .onErrorReturn(e -> {
                                System.out.println("Connection closed, unsubscribing from Hystrix Stream");
                                s.unsubscribe();
                                return null;
                            });
                }).subscribe());
            });
        }, PipelineConfigurators.<ByteBuf> serveSseConfigurator()).start();
    }

    final static Observable<String> streamPoller = Observable.create((Subscriber<? super String> s) -> {
        //        try {
        //            System.out.println("Server => Start Hystrix Metric Poller");
        //            HystrixMetricsPoller poller = new HystrixMetricsPoller(json -> {
        //                s.onNext(json);
        //            }, 1000);
        //            s.add(Subscriptions.create(() -> {
        //                System.out.println("Server => Shutdown Hystrix Stream");
        //                poller.shutdown();
        //            }));
        //            poller.start();
        //        } catch (Exception e) {
        //            s.onError(e);
        //        }
        }).publish().refCount();

    public static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<ByteBuf> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message);
    }

}
