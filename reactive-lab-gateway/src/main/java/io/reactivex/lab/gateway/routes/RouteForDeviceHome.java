package io.reactivex.lab.gateway.routes;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.StartGatewayServer;
import io.reactivex.lab.gateway.clients.BookmarkCommand;
import io.reactivex.lab.gateway.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.gateway.clients.LoadBalancerFactory;
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
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteForDeviceHome {

    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> bookmarksLb;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> userLb;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> personalizationLb;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> ratingsLb;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> videoMetadataLb;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> socialLb;

    public RouteForDeviceHome(LoadBalancerFactory loadBalancerFactory) {
        bookmarksLb = loadBalancerFactory.forVip("reactive-lab-bookmark-service");
        userLb = loadBalancerFactory.forVip("reactive-lab-user-service");
        personalizationLb = loadBalancerFactory.forVip("reactive-lab-personalized-catalog-service");
        ratingsLb = loadBalancerFactory.forVip("reactive-lab-ratings-service");
        videoMetadataLb = loadBalancerFactory.forVip("reactive-lab-vms-service");
        socialLb = loadBalancerFactory.forVip("reactive-lab-social-service");
    }

    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        List<String> userId = request.getQueryParameters().get("userId");
        if (userId == null || userId.size() != 1) {
            return StartGatewayServer.writeError(request, response, "A single 'userId' is required.");
        }

        return new UserCommand(userId, userLb).observe().flatMap(user -> {
            Observable<Map<String, Object>> catalog = new PersonalizedCatalogCommand(user, personalizationLb).observe()
                                                                                                             .flatMap(
                                                                                                                     catalogList -> catalogList
                                                                                                                             .videos()
                                                                                                                             .<Map<String, Object>>flatMap(
                                                                                                                                     video -> {
                                                                                                                                         Observable<Bookmark>
                                                                                                                                                 bookmark =
                                                                                                                                                 new BookmarkCommand(
                                                                                                                                                         video,
                                                                                                                                                         bookmarksLb)
                                                                                                                                                         .observe()
                                                                                                                                                         .doOnEach(
                                                                                                                                                                 not -> System.out
                                                                                                                                                                         .println(
                                                                                                                                                                                 "bookmark=>"
                                                                                                                                                                                 + not.getKind()));
                                                                                                                                         Observable<Rating>
                                                                                                                                                 rating =
                                                                                                                                                 new RatingsCommand(
                                                                                                                                                         video,
                                                                                                                                                         ratingsLb)
                                                                                                                                                         .observe()
                                                                                                                                                         .doOnEach(
                                                                                                                                                                 not -> System.out
                                                                                                                                                                         .println(
                                                                                                                                                                                 "rating=>"
                                                                                                                                                                                 + not.getKind()));
                                                                                                                                         Observable<VideoMetadata>
                                                                                                                                                 metadata =
                                                                                                                                                 new VideoMetadataCommand(
                                                                                                                                                         video,
                                                                                                                                                         videoMetadataLb)
                                                                                                                                                         .observe()
                                                                                                                                                         .doOnEach(
                                                                                                                                                                 not -> System.out
                                                                                                                                                                         .println(
                                                                                                                                                                                 "video=>"
                                                                                                                                                                                 + not.getKind()));
                                                                                                                                         ;
                                                                                                                                         return Observable
                                                                                                                                                 .zip(bookmark,
                                                                                                                                                      rating,
                                                                                                                                                      metadata,
                                                                                                                                                      (b,
                                                                                                                                                       r,
                                                                                                                                                       m) -> combineVideoData(
                                                                                                                                                              video,
                                                                                                                                                              b,
                                                                                                                                                              r,
                                                                                                                                                              m))
                                                                                                                                                 .doOnNext(
                                                                                                                                                         stringObjectMap -> {
                                                                                                                                                             System.out
                                                                                                                                                                     .println(
                                                                                                                                                                             "stringObjectMap = "
                                                                                                                                                                             + stringObjectMap);
                                                                                                                                                         });
                                                                                                                                     }));

            Observable<Map<String, Object>> social = new SocialCommand(user, socialLb).observe()
                                                                                      .map(SocialCommand.Social::getDataAsMap)
                                                                                      .doOnNext(System.out::println);

            return Observable.merge(catalog, social);
        }).flatMap(data -> {
            String json = SimpleJson.mapToJson(data);
            System.out.println("json = " + json);
            return response.writeStringAndFlush("data: " + json);
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
