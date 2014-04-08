package io.reactivex.lab.middle.nf.services;

import io.reactivex.lab.common.SimpleJson;
import io.reactivex.lab.middle.nf.MiddleTierService;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import rx.Observable;

public class VideoMetadataService extends MiddleTierService {

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        List<String> videoIds = request.getQueryParameters().get("videoId");
        return Observable.from(videoIds).map(videoId -> {
            Map<String, Object> video = new HashMap<>();
            video.put("videoId", videoId);
            video.put("title", "Video Title");
            video.put("other_data", "goes_here");
            return video;
        }).flatMap(video -> {
            return response.writeAndFlush(new ServerSentEvent("", "data", SimpleJson.mapToJson(video)));
        }).delay(((long) (Math.random() * 20) + 20), TimeUnit.MILLISECONDS); // simulate latency 
    }
}
