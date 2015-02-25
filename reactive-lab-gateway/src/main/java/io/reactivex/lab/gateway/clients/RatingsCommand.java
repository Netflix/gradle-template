package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.RatingsCommand.Rating;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.lab.gateway.loadbalancer.DiscoveryAndLoadBalancer;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RatingsCommand extends HystrixObservableCommand<Rating> {
    private final List<Video> videos;
    private static final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer =
            DiscoveryAndLoadBalancer.getFactory().forVip("reactive-lab-ratings-service");

    public RatingsCommand(Video video) {
        this(Arrays.asList(video));
        // replace with HystrixCollapser
    }

    public RatingsCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("Ratings"));
        this.videos = videos;
    }

    @Override
    protected Observable<Rating> construct() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/ratings?" + UrlGenerator.generate("videoId", videos));
        return loadBalancer.choose()
                           .map(holder -> holder.getClient())
                           .<Rating>flatMap(client -> client.submit(request)
                                                    .flatMap(r -> r.getContent()
                                                                   .map((ServerSentEvent sse) -> Rating.fromJson(sse.contentAsString()))))
                           .retry(1);
    }

    @Override
    protected Observable<Rating> resumeWithFallback() {
        Map<String, Object> video = new HashMap<>();
        video.put("videoId", videos.get(0).getId());
        video.put("estimated_user_rating", 3.5);
        video.put("actual_user_rating", 4);
        video.put("average_user_rating", 3.1);
        return Observable.just(new Rating(video));
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
