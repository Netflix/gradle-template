package io.reactivex.lab.gateway.routes;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.client.resolver.ServerResolvers;
import com.netflix.eureka2.interests.Interests;
import com.netflix.eureka2.transport.EurekaTransports;
import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.lab.gateway.StartGatewayServer;
import io.reactivex.lab.gateway.clients.BookmarkCommand;
import io.reactivex.lab.gateway.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.gateway.clients.MiddleTierLoadBalancer;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.RatingsCommand;
import io.reactivex.lab.gateway.clients.RatingsCommand.Rating;
import io.reactivex.lab.gateway.clients.SocialCommand;
import io.reactivex.lab.gateway.clients.UserCommand;
import io.reactivex.lab.gateway.clients.VideoMetadataCommand;
import io.reactivex.lab.gateway.clients.VideoMetadataCommand.VideoMetadata;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.Host;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.eureka.EurekaMembershipSource;
import netflix.ocelli.rxnetty.HttpClientHolder;
import netflix.ocelli.rxnetty.HttpClientPool;
import rx.Observable;
import rx.functions.Func1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.reactivex.netty.pipeline.PipelineConfigurators.clientSseConfigurator;

public class RouteForDeviceHome {

    private static final RouteForDeviceHome INSTANCE = new RouteForDeviceHome();
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> bookmarksLb;

    private RouteForDeviceHome() {
        ServerResolver.Server discoveryServer = new ServerResolver.Server("127.0.0.1", 7001);
        ServerResolver.Server registrationServer = new ServerResolver.Server("127.0.0.1", 7001);
        EurekaClient client = Eureka.newClientBuilder(ServerResolvers.from(discoveryServer),
                                                      ServerResolvers.from(registrationServer))
                                    .withCodec(EurekaTransports.Codec.Json)
                                    .build();

        client.forInterest(Interests.forFullRegistry()).take(1).toBlocking().single();

        EurekaMembershipSource membershipSource = new EurekaMembershipSource(client);
        MiddleTierLoadBalancer loadBalancer = new MiddleTierLoadBalancer(membershipSource,
                                                  new HttpClientPool<ByteBuf, ServerSentEvent>(new Func1<Host, HttpClient<ByteBuf, ServerSentEvent>>() {
                                                      @Override
                                                      public HttpClient<ByteBuf, ServerSentEvent> call(Host host) {
                                                          return RxNetty.<ByteBuf, ServerSentEvent>newHttpClientBuilder(host.getHostName(), host.getPort())
                                                                        .pipelineConfigurator(clientSseConfigurator())
                                                                        .enableWireLogging(LogLevel.ERROR)
                                                                        .build();
                                                      }
                                                  }));
        bookmarksLb = loadBalancer.forVip("reactive-lab-bookmark-service");
    }

    public static RouteForDeviceHome getInstance() {
        return INSTANCE;
    }

    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        List<String> userId = request.getQueryParameters().get("userId");
        if (userId == null || userId.size() != 1) {
            return StartGatewayServer.writeError(request, response, "A single 'userId' is required.");
        }

        return new UserCommand(userId).observe().flatMap(user -> {
            Observable<Map<String, Object>> catalog = new PersonalizedCatalogCommand(user).observe()
                    .flatMap(catalogList -> catalogList.videos().<Map<String, Object>> flatMap(video -> {
                        Observable<Bookmark> bookmark = new BookmarkCommand(video, bookmarksLb).observe();
                        Observable<Rating> rating = new RatingsCommand(video).observe();
                        Observable<VideoMetadata> metadata = new VideoMetadataCommand(video).observe();
                        return Observable.zip(bookmark, rating, metadata, (b, r, m) -> combineVideoData(video, b, r, m));
                    }));

            Observable<Map<String, Object>> social = new SocialCommand(user).observe()
                                                                            .map(SocialCommand.Social::getDataAsMap);

            return Observable.merge(catalog, social);
        }).flatMap(data -> response.writeStringAndFlush("data: " + SimpleJson.mapToJson(data)));
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
