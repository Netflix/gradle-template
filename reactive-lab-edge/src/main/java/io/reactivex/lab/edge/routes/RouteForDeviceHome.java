package io.reactivex.lab.edge.routes;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.edge.EdgeServer;
import io.reactivex.lab.edge.clients.BookmarkCommand;
import io.reactivex.lab.edge.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.edge.clients.PersonalizedCatalogCommand;
import io.reactivex.lab.edge.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.edge.clients.RatingsCommand;
import io.reactivex.lab.edge.clients.RatingsCommand.Rating;
import io.reactivex.lab.edge.clients.SocialCommand;
import io.reactivex.lab.edge.clients.UserCommand;
import io.reactivex.lab.edge.clients.VideoMetadataCommand;
import io.reactivex.lab.edge.clients.VideoMetadataCommand.VideoMetadata;
import io.reactivex.lab.edge.common.SimpleJson;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;

public class RouteForDeviceHome {

    private static final RouteForDeviceHome INSTANCE = new RouteForDeviceHome();

    private RouteForDeviceHome() {

    }

    public static RouteForDeviceHome getInstance() {
        return INSTANCE;
    }

    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        List<String> userId = request.getQueryParameters().get("userId");
        if (userId == null || userId.size() != 1) {
            return EdgeServer.writeError(request, response, "A single 'userId' is required.");
        }

        return new UserCommand(userId).observe().flatMap(user -> {
            Observable<Map<String, Object>> catalog = new PersonalizedCatalogCommand(user).observe()
                    .flatMap(catalogList -> {
                        return catalogList.videos().<Map<String, Object>> flatMap(video -> {
                            Observable<Bookmark> bookmark = new BookmarkCommand(video).observe();
                            Observable<Rating> rating = new RatingsCommand(video).observe();
                            Observable<VideoMetadata> metadata = new VideoMetadataCommand(video).observe();
                            return Observable.zip(bookmark, rating, metadata, (b, r, m) -> {
                                return combineVideoData(video, b, r, m);
                            });
                        });
                    });

            Observable<Map<String, Object>> social = new SocialCommand(user).observe().map(s -> {
                return s.getDataAsMap();
            });

            return Observable.merge(catalog, social);
        }).flatMap(data -> {
            return response.writeAndFlush(new ServerSentEvent("", "data", SimpleJson.mapToJson(data)), EdgeServer.SSE_TRANSFORMER);
        });
    }

    private Map<String, Object> combineVideoData(Video video, Bookmark b, Rating r, VideoMetadata m) {
        Map<String, Object> video_data = new HashMap<>();
        video_data.put("video_id", video.getId());
        video_data.put("bookmark", b.getPosition());
        video_data.put("estimated_user_rating", r.getEstimatedUserRating());
        video_data.put("metadata", m.getDataAsMap());
        return video_data;
    }
}
