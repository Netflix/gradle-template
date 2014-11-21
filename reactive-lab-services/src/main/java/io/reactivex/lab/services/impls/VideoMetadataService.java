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

public class VideoMetadataService extends AbstractMiddleTierService {

    public VideoMetadataService(EurekaClient client) {
        super("reactive-lab-vms-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        List<String> videoIds = request.getQueryParameters().get("videoId");
        return Observable.from(videoIds).map(videoId -> {
            Map<String, Object> video = new HashMap<>();
            video.put("videoId", videoId);
            video.put("title", "Video Title");
            video.put("other_data", "goes_here");
            return video;
        }).flatMap(video -> response.writeStringAndFlush("data: " + SimpleJson.mapToJson(video) + "\n")
                .doOnCompleted(response::close))
                .delay(((long) (Math.random() * 20) + 20), TimeUnit.MILLISECONDS); // simulate latency
    }
}
