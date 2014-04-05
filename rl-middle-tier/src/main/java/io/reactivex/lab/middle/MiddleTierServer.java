package io.reactivex.lab.middle;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;

import rx.Observable;

public class MiddleTierServer {

    public static void main(String... args) {
        RxNetty.createHttpServer(9090, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            try {
                if (request.getPath().equals("/mock.json")) {
                    return generateMockResponse(request, response)
                            .doOnCompleted(() -> System.out.println("Server => Successful Response"));
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

    private static Observable<Void> generateMockResponse(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) throws IOException, JsonGenerationException {
        List<String> _id = request.getQueryParameters().get("id");
        if (_id == null || _id.size() != 1) {
            return writeError(request, response, "Please provide a numerical 'id' value. It can be a random number (uuid). Received => " + _id);
        }
        long id = Long.parseLong(String.valueOf(_id.get(0)));

        int delay = getParameter(request, "delay", 50); // default to 50ms server-side delay
        int itemSize = getParameter(request, "itemSize", 128); // default to 128 bytes item size (assuming ascii text)
        int numItems = getParameter(request, "numItems", 10); // default to 10 items in a list

        // no more than 100 items
        if (numItems < 1 || numItems > 100) {
            return writeError(request, response, "Please choose a 'numItems' value from 1 to 100.");
        }

        // no larger than 50KB per item
        if (itemSize < 1 || itemSize > 1024 * 50) {
            return writeError(request, response, "Please choose an 'itemSize' value from 1 to 1024*50 (50KB).");
        }

        // no larger than 60 second delay
        if (delay < 0 || delay > 60000) {
            return writeError(request, response, "Please choose a 'delay' value from 0 to 60000 (60 seconds).");
        }

        response.setStatus(HttpResponseStatus.OK);
        return MockResponse.generateJson(id, delay, itemSize, numItems).flatMap(json -> response.writeStringAndFlush(json));
    }

    private static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<?> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message + "\n");
    }

    private static int getParameter(HttpServerRequest<?> request, String key, int defaultValue) {
        List<String> v = request.getQueryParameters().get(key);
        if (v == null || v.size() != 1) {
            return defaultValue;
        } else {
            return Integer.parseInt(String.valueOf(v.get(0)));
        }
    }
}
