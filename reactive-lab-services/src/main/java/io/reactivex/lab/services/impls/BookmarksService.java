package io.reactivex.lab.services.impls;

import com.netflix.eureka2.client.EurekaClient;

import io.reactivex.lab.services.common.Random;
import io.reactivex.lab.services.common.SimpleJson;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class BookmarksService extends AbstractMiddleTierService {

    public BookmarksService(EurekaClient client) {
        super("reactive-lab-bookmark-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        List<String> videoIds = request.getQueryParameters().get("videoId");

        int latency = 1;
        if (Random.randomIntFrom0to100() > 80) {
            latency = 10;
        }

        return Observable.from(videoIds).map(videoId -> {
            Map<String, Object> video = new HashMap<>();
            video.put("videoId", videoId);
            video.put("position", (int) (Math.random() * 5000));
            return video;
        }).flatMap(video -> response.writeStringAndFlush("data: " + SimpleJson.mapToJson(video) + "\n"))
                .delay(latency, TimeUnit.MILLISECONDS).doOnCompleted(response::close); // simulate latency
    }
}
