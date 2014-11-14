package io.reactivex.lab.gateway.routes;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.StartGatewayServer;
import io.reactivex.lab.gateway.clients.BookmarkCommand;
import io.reactivex.lab.gateway.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.RatingsCommand;
import io.reactivex.lab.gateway.clients.RatingsCommand.Rating;
import io.reactivex.lab.gateway.clients.SocialCommand;
import io.reactivex.lab.gateway.clients.UserCommand;
import io.reactivex.lab.gateway.clients.VideoMetadataCommand;
import io.reactivex.lab.gateway.clients.VideoMetadataCommand.VideoMetadata;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;

public class RouteForDeviceHome {

    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        List<String> userId = request.getQueryParameters().get("userId");
        if (userId == null || userId.size() != 1) {
            return StartGatewayServer.writeError(request, response, "A single 'userId' is required.");
        }

        return new UserCommand(userId).observe().flatMap(user -> {
            Observable<Map<String, Object>> catalog = new PersonalizedCatalogCommand(user).observe()
                    .flatMap(catalogList -> catalogList.videos().<Map<String, Object>> flatMap(
                            video -> {
                                Observable<Bookmark> bookmark = new BookmarkCommand(video).observe();
                                Observable<Rating> rating = new RatingsCommand(video).observe();
                                Observable<VideoMetadata> metadata = new VideoMetadataCommand(video).observe();
                                return Observable.zip(bookmark, rating, metadata, (b, r, m) -> combineVideoData(video, b, r, m));
                            }));

            Observable<Map<String, Object>> social = new SocialCommand(user).observe().map(s -> {
                return s.getDataAsMap();
            });

            return Observable.merge(catalog, social);
        }).flatMap(data -> {
            String json = SimpleJson.mapToJson(data);
            return response.writeStringAndFlush("data: " + json + "\n");
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
