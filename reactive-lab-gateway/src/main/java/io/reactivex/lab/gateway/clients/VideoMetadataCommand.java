package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.VideoMetadataCommand.VideoMetadata;
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

public class VideoMetadataCommand extends HystrixObservableCommand<VideoMetadata> {

    private final List<Video> videos;
    private static final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer =
            DiscoveryAndLoadBalancer.getFactory().forVip("reactive-lab-vms-service");

    public VideoMetadataCommand(Video video) {
        this(Arrays.asList(video));
        // replace with HystrixCollapser
    }

    public VideoMetadataCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("VideoMetadata"));
        this.videos = videos;
    }

    @Override
    protected Observable<VideoMetadata> construct() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/metadata?" + UrlGenerator.generate("videoId",
                                                                                                              videos));
        return loadBalancer.choose()
                           .map(holder -> holder.getClient())
                           .<VideoMetadata>flatMap(client -> client.submit(request)
                                                    .flatMap(r -> r.getContent()
                                                                   .map((ServerSentEvent sse) -> VideoMetadata.fromJson(sse.contentAsString()))))
                           .retry(1);
    }
    
    @Override
    protected Observable<VideoMetadata> resumeWithFallback() {
        Map<String, Object> video = new HashMap<>();
        video.put("videoId", videos.get(0).getId());
        video.put("title", "Fallback Video Title");
        video.put("other_data", "goes_here");
        return Observable.just(new VideoMetadata(video));
    }

    public static class VideoMetadata {

        private final Map<String, Object> data;

        public VideoMetadata(Map<String, Object> data) {
            this.data = data;
        }

        public static VideoMetadata fromJson(String json) {
            return new VideoMetadata(SimpleJson.jsonToMap(json));
        }

        public int getId() {
            return (int) data.get("videoId");
        }

        public String getTitle() {
            return (String) data.get("title");
        }

        public Map<String, Object> getDataAsMap() {
            return data;
        }

    }

}
