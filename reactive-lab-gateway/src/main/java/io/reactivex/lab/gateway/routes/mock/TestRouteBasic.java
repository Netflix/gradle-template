package io.reactivex.lab.gateway.routes.mock;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.lab.gateway.common.RxNettyResponseWriter;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.Arrays;
import java.util.List;

import rx.Observable;

/**
 * Handle request and return a response that composes multiple backend services like this:
 * 
 * A) GET http://hostname:9100/mock.json?numItems=2&itemSize=50&delay=50&id={uuid}
 * B) GET http://hostname:9100/mock.json?numItems=25&itemSize=30&delay=150&id={uuid}
 * C) GET http://hostname:9100/mock.json?numItems=1&itemSize=5000&delay=80&id={a.responseKey}
 * D) GET http://hostname:9100/mock.json?numItems=1&itemSize=1000&delay=1&id={a.responseKey}
 * E) GET http://hostname:9100/mock.json?numItems=100&itemSize=30&delay=4&id={b.responseKey}
 */
public class TestRouteBasic {

    public static Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        List<String> _id = request.getQueryParameters().get("id");
        if (_id == null || _id.size() != 1) {
            return writeError(request, response, "Please provide a numerical 'id' value. It can be a random number (uuid).");
        }
        long id = Long.parseLong(String.valueOf(_id.get(0)));

        // set response header
        response.getHeaders().addHeader("content-type", "application/json");

        Observable<List<BackendResponse>> acd = getDataFromBackend("/mock.json?numItems=2&itemSize=50&delay=50&id=" + id)
                // Eclipse 20140224-0627 can't infer without this type hint even though the Java 8 compiler can
                .<List<BackendResponse>> flatMap(responseA -> {
                    Observable<BackendResponse> responseC = getDataFromBackend("/mock.json?numItems=1&itemSize=5000&delay=80&id=" + responseA.getResponseKey());
                    Observable<BackendResponse> responseD = getDataFromBackend("/mock.json?numItems=1&itemSize=1000&delay=1&id=" + responseA.getResponseKey());
                    return Observable.zip(Observable.just(responseA), responseC, responseD, (a, c, d) -> Arrays.asList(a, c, d));
                });

        Observable<List<BackendResponse>> be = getDataFromBackend("/mock.json?numItems=25&itemSize=30&delay=150&id=" + id)
                // Eclipse 20140224-0627 can't infer without this type hint even though the Java 8 compiler can
                .<List<BackendResponse>> flatMap(responseB -> {
                    Observable<BackendResponse> responseE = getDataFromBackend("/mock.json?numItems=100&itemSize=30&delay=4&id=" + responseB.getResponseKey());
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

    private static Observable<BackendResponse> getDataFromBackend(String url) {
        return RxNetty.createHttpClient("localhost", 9100)
                .submit(HttpClientRequest.createGet(url))
                .flatMap((HttpClientResponse<ByteBuf> r) -> {
                    Observable<BackendResponse> bytesToJson = r.getContent().map(b -> {
                        return BackendResponse.fromJson(new ByteBufInputStream(b));
                    });
                    return bytesToJson;
                });
    }

    private static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<?> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message + "\n");
    }
}
