package io.reactivex.lab.gateway.clients;

import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.RatingsCommand.Rating;
import io.reactivex.lab.gateway.common.RxNettySSE;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

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
        return RxNettySSE.createHttpClient("localhost", 9193)
                .submit(HttpClientRequest.createGet("/ratings?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> {
                    Observable<Rating> bytesToJson = r.getContent().map(sse -> {
                        return Rating.fromJson(sse.contentAsString());
                    });
                    return bytesToJson;
                });
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
