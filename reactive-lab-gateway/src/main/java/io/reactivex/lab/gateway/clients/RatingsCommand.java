package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.RatingsCommand.Rating;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RatingsCommand extends HystrixObservableCommand<Rating> {
    private final List<Video> videos;

    public RatingsCommand(Video video) {
        this(Arrays.asList(video));
        // replace with HystrixCollapser
    }

    public RatingsCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("Ratings"));
        this.videos = videos;
    }

    @Override
    protected Observable<Rating> run() {
        return RxNetty.createHttpClient("localhost", 9193, PipelineConfigurators.<ByteBuf>clientSseConfigurator())
                .submit(HttpClientRequest.createGet("/ratings?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> r.getContent().map(sse -> {
                    String ratings = sse.contentAsString();
                    System.out.println("ratings = " + ratings);
                    return Rating.fromJson(ratings);
                }));
    }

    public static class Rating {

        private final Map<String, Object> data;

        private Rating(Map<String, Object> data) {
            this.data = data;
        }

        public double getEstimatedUserRating() {
            return (double) data.get("estimated_user_rating");
        }

        public double getActualUserRating() {
            return (double) data.get("actual_user_rating");
        }

        public double getAverageUserRating() {
            return (double) data.get("average_user_rating");
        }

        public static Rating fromJson(String json) {
            return new Rating(SimpleJson.jsonToMap(json));
        }

    }
}
