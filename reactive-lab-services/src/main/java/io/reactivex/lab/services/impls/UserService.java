package io.reactivex.lab.services.impls;

import com.netflix.eureka2.client.EurekaClient;

import io.reactivex.lab.services.common.SimpleJson;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class UserService extends AbstractMiddleTierService {

    public UserService(EurekaClient client) {
        super("reactive-lab-user-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        List<String> userIds = request.getQueryParameters().get("userId");
        if (userIds == null || userIds.size() == 0) {
            return writeError(request, response, "At least one parameter of 'userId' must be included.");
        }
        return Observable.from(userIds).map(userId -> {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", userId);
            user.put("name", "Name Here");
            user.put("other_data", "goes_here");
            return user;
        }).flatMap(user -> response.writeStringAndFlush("data: " + SimpleJson.mapToJson(user) + "\n")
                .doOnCompleted(response::close))
                .delay(((long) (Math.random() * 500) + 500), TimeUnit.MILLISECONDS); // simulate latency
    }
}
