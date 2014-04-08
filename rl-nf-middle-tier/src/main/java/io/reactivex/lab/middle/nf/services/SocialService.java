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

public class SocialService extends MiddleTierService {

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        return Observable.from(request.getQueryParameters().get("userId")).map(userId -> {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", userId);
            user.put("friends", Arrays.asList(randomUser(), randomUser(), randomUser(), randomUser()));
            return user;
        }).flatMap(list -> {
            return response.writeAndFlush(new ServerSentEvent("", "data", SimpleJson.mapToJson(list)));
        }).delay(((long) (Math.random() * 100) + 20), TimeUnit.MILLISECONDS); // simulate latency 
    }

    private static int randomUser() {
        return ((int) (Math.random() * 10000));
    }
}
