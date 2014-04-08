package io.reactivex.lab.middle.nf.services;

import io.reactivex.lab.common.SimpleJson;
import io.reactivex.lab.middle.nf.MiddleTierService;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;

public class PersonalizedCatalogService extends MiddleTierService {

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        return Observable.from(request.getQueryParameters().get("userId")).map(userId -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("user_id", userId);

            userData.put("list_title", "Really quirky and over detailed list title!");
            userData.put("other_data", "goes_here");
            userData.put("videos", Arrays.asList(12345, 23456, 34567, 45678, 56789, 67890));
            return userData;
        }).flatMap(list -> {
            return response.writeAndFlush(new ServerSentEvent("", "data", SimpleJson.mapToJson(list)));
        }).delay(((long) (Math.random() * 100) + 20), TimeUnit.MILLISECONDS); // simulate latency 
    }
}
