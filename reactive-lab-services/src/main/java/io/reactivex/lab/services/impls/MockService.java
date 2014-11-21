package io.reactivex.lab.services.impls;

import com.netflix.eureka2.client.EurekaClient;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;

import java.util.List;

public class MockService extends AbstractMiddleTierService {

    public MockService(EurekaClient client) {
        super("reactive-lab-mock-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
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
        return MockResponse.generateJson(id, delay, itemSize, numItems)
                .flatMap(json -> response.writeStringAndFlush("data:" + json + "\n"))
                .doOnCompleted(response::close);
    }

}
