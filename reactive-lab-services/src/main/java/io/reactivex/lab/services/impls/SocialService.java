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

public class SocialService extends AbstractMiddleTierService {

    public SocialService(EurekaClient client) {
        super("reactive-lab-social-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        return Observable.from(request.getQueryParameters().get("userId")).map(userId -> {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", userId);
            user.put("friends", Arrays.asList(randomUser(), randomUser(), randomUser(), randomUser()));
            return user;
        }).flatMap(list -> response.writeStringAndFlush("data: " + SimpleJson.mapToJson(list) + "\n"))
                .delay(((long) (Math.random() * 100) + 20), TimeUnit.MILLISECONDS).doOnCompleted(response::close); // simulate latency
    }

    private static int randomUser() {
        return ((int) (Math.random() * 10000));
    }
}
