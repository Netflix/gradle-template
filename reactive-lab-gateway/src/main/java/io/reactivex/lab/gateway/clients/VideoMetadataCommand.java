package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.VideoMetadataCommand.VideoMetadata;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VideoMetadataCommand extends HystrixObservableCommand<VideoMetadata> {

    private final List<Video> videos;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer;

    public VideoMetadataCommand(Video video, LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer) {
        this(Arrays.asList(video), loadBalancer);
        // replace with HystrixCollapser
    }

    public VideoMetadataCommand(List<Video> videos, LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer) {
        super(HystrixCommandGroupKey.Factory.asKey("VideoMetadata"));
        this.videos = videos;
        this.loadBalancer = loadBalancer;
    }

    @Override
    protected Observable<VideoMetadata> run() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/metadata?" + UrlGenerator.generate("videoId",
                                                                                                              videos));
        return loadBalancer.choose().map(holder -> holder.getClient())
                           .flatMap(client -> client.submit(request)
                                                    .flatMap(r -> r.getContent().map(sse -> {
                                                        String videometa = sse.contentAsString();
                                                        System.out.println("videometa = " + videometa);
                                                        return VideoMetadata.fromJson(videometa);
                                                    })));
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
