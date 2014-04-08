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

public class BookmarksService extends MiddleTierService {

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        List<String> videoIds = request.getQueryParameters().get("videoId");
        return Observable.from(videoIds).map(videoId -> {
            Map<String, Object> video = new HashMap<>();
            video.put("videoId", videoId);
            video.put("position", (int) (Math.random() * 5000));
            return video;
        }).flatMap(video -> {
            return response.writeAndFlush(new ServerSentEvent("", "data", SimpleJson.mapToJson(video)));
        }).delay(1, TimeUnit.MILLISECONDS); // simulate latency
    }
}
