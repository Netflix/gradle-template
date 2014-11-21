package io.reactivex.lab.services.impls;

import com.netflix.eureka2.client.EurekaClient;

import io.reactivex.lab.services.common.SimpleJson;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PersonalizedCatalogService extends AbstractMiddleTierService {

    public PersonalizedCatalogService(EurekaClient client) {
        super("reactive-lab-personalized-catalog-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        return Observable.from(request.getQueryParameters().get("userId")).map(userId -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", userId);

            userData.put("list_title", "Really quirky and over detailed list title!");
            userData.put("other_data", "goes_here");
            userData.put("videos", Arrays.asList(12345, 23456, 34567, 45678, 56789, 67890));
            return userData;
        }).flatMap(list -> response.writeStringAndFlush("data: " + SimpleJson.mapToJson(list) + "\n"))
                .delay(((long) (Math.random() * 100) + 20), TimeUnit.MILLISECONDS)
                .doOnCompleted(response::close); // simulate latency
    }
}
