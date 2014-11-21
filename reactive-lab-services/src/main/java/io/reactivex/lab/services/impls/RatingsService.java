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

public class RatingsService extends AbstractMiddleTierService {

    public RatingsService(EurekaClient client) {
        super("reactive-lab-ratings-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        List<String> videoIds = request.getQueryParameters().get("videoId");
        return Observable.from(videoIds).map(videoId -> {
            Map<String, Object> video = new HashMap<>();
            video.put("videoId", videoId);
            video.put("estimated_user_rating", 3.5);
            video.put("actual_user_rating", 4);
            video.put("average_user_rating", 3.1);
            return video;
        }).flatMap(video -> response.writeStringAndFlush("data: " + SimpleJson.mapToJson(video) + "\n"))
                .delay(20, TimeUnit.MILLISECONDS).doOnCompleted(response::close); // simulate latenc
    }
}
