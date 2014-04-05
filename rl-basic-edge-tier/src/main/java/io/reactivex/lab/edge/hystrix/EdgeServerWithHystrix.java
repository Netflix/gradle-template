package io.reactivex.lab.edge.hystrix;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.lab.edge.common.BackendResponse;
import io.reactivex.lab.edge.common.ResponseBuilder;
import io.reactivex.lab.edge.common.RxNettyResponseWriter;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.Arrays;
import java.util.List;

import rx.Observable;

import com.netflix.hystrix.HystrixRequestLog;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;

public class EdgeServerWithHystrix {

    public static void main(String... args) {
        RxNetty.createHttpServer(8080, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            /** wrap in outer Observable so we have a consistent 'finallyDo' at the end for the context */
            return Observable.just(request).flatMap(r -> {
                HystrixRequestContext.initializeContext();
                try {
                    if (request.getPath().equals("/test")) {
                        return testEndpoint(request, response).onErrorFlatMap(error -> {
                            return writeError(request, response, "Unknown error: " + error.getMessage());
                        });
                    } else {
                        return writeError(request, response, "Unknown path: " + request.getPath());
                    }
                } catch (Throwable e) {
                    System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                    response.setStatus(HttpResponseStatus.BAD_REQUEST);
                    return response.writeStringAndFlush("Error 500: Bad Request\n" + e.getMessage() + "\n");
                }
            }).finallyDo(() -> {
                System.out.println("Response => " + HystrixRequestLog.getCurrentRequest().getExecutedCommandsAsString());
                HystrixRequestContext.getContextForCurrentThread().shutdown();
            });

        }).startAndWait();
    }

    private static Observable<Void> testEndpoint(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        List<String> _id = request.getQueryParameters().get("id");
        if (_id == null || _id.size() != 1) {
            return writeError(request, response, "Please provide a numerical 'id' value. It can be a random number (uuid).");
        }
        long id = Long.parseLong(String.valueOf(_id.get(0)));

        // set response header
        response.getHeaders().addHeader("content-type", "application/json");

        Observable<List<BackendResponse>> acd = new MiddleTierClientHystrixCommand(id, 2, 50, 50).observe()
                // Eclipse 20140224-0627 can't infer without this type hint even though the Java 8 compiler can
                .<List<BackendResponse>> flatMap(responseA -> {
                    Observable<BackendResponse> responseC = new MiddleTierClientHystrixCommand(responseA.getResponseKey(), 1, 5000, 80).observe();
                    Observable<BackendResponse> responseD = new MiddleTierClientHystrixCommand(responseA.getResponseKey(), 1, 1000, 1).observe();
                    return Observable.zip(Observable.just(responseA), responseC, responseD, (a, c, d) -> Arrays.asList(a, c, d));
                });

        Observable<List<BackendResponse>> be = new MiddleTierClientHystrixCommand(id, 25, 30, 15).observe()
                // Eclipse 20140224-0627 can't infer without this type hint even though the Java 8 compiler can
                .<List<BackendResponse>> flatMap(responseB -> {
                    Observable<BackendResponse> responseE = new MiddleTierClientHystrixCommand(responseB.getResponseKey(), 100, 30, 4).observe();
                    return Observable.zip(Observable.just(responseB), responseE, (b, e) -> Arrays.asList(b, e));
                });

        return Observable.zip(acd, be, (_acd, _be) -> {
            BackendResponse responseA = _acd.get(0);
            BackendResponse responseB = _be.get(0);
            BackendResponse responseC = _acd.get(1);
            BackendResponse responseD = _acd.get(2);
            BackendResponse responseE = _be.get(1);

            /**
             * The RxNettyResponseWriter bridges synchronous Jackson JSON writing
             * with asynchronous Netty writing.
             */
            RxNettyResponseWriter writer = new RxNettyResponseWriter(response);
            ResponseBuilder.writeTestResponse(writer, responseA, responseB, responseC, responseD, responseE);
            return writer;
        }).flatMap(w -> w.asObservable());
    }

    private static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<?> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message + "\n");
    }

}
