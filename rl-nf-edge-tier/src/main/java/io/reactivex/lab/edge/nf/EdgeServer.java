package io.reactivex.lab.edge.nf;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;

public class EdgeServer {

    public static void main(String... args) {
        RxNetty.createHttpServer(8080, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            try {
                if (request.getPath().equals("/device/home")) {
                    return EndpointForDeviceHome.getInstance().handle(request, response).onErrorResumeNext(error -> {
                        error.printStackTrace();
                        return writeError(request, response, "Failed: " + error.getMessage());
                    });
                } else {
                    return writeError(request, response, "Unknown path: " + request.getPath());
                }
            } catch (Throwable e) {
                System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                return response.writeStringAndFlush("Error 500: Bad Request\n" + e.getMessage() + "\n");
            }
        }, PipelineConfigurators.<ByteBuf> sseServerConfigurator()).startAndWait();
    }

    public static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<?> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message + "\n");
    }
}
