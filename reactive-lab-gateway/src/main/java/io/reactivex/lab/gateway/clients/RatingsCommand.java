package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.RatingsCommand.Rating;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class RatingsCommand extends HystrixObservableCommand<Rating> {
    private final List<Video> videos;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer;

    public RatingsCommand(Video video, LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer) {
        this(Arrays.asList(video), loadBalancer);
        // replace with HystrixCollapser
    }

    public RatingsCommand(List<Video> videos, LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer) {
        super(HystrixCommandGroupKey.Factory.asKey("Ratings"));
        this.videos = videos;
        this.loadBalancer = loadBalancer;
    }

    @Override
    protected Observable<Rating> run() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/ratings?" + UrlGenerator.generate("videoId",
                                                                                                             videos));
        return loadBalancer.choose().map(holder -> holder.getClient())
                           .flatMap(client -> {
                               System.out.println("RatingsCommand.run");
                               return client.submit(request)
                                            .flatMap(r -> {
                                                System.out.println("RatingsCommand.response" + r.getStatus());
                                                return r.getContent().map(sse -> {
                                                    System.out.println("RatingsCommand.content");
                                                    String ratings = sse.contentAsString();
                                                    System.out.println("ratings = " + ratings);
                                                    return Rating.fromJson(ratings);
                                                });
                                            });
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
/*

    public static void main(String[] args) {
        RxNetty.createHttpClient("127.0.0.1", 9193, PipelineConfigurators.<ByteBuf>clientSseConfigurator())
               .submit(HttpClientRequest.createGet("?id=1&videoId=2")).flatMap().toBlocking()
    }
*/
}
