package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.clients.VideoMetadataCommand.VideoMetadata;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class VideoMetadataCommand extends HystrixObservableCommand<VideoMetadata> {

    private final List<Video> videos;

    public VideoMetadataCommand(Video video) {
        this(Arrays.asList(video));
        // replace with HystrixCollapser
    }

    public VideoMetadataCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("VideoMetadata"));
        this.videos = videos;
    }

    @Override
    protected Observable<VideoMetadata> run() {
        return RxNetty.createHttpClient("localhost", 9196, PipelineConfigurators.<ByteBuf> clientSseConfigurator())
                .submit(HttpClientRequest.createGet("/metadata?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> r.getContent().map(sse -> VideoMetadata.fromJson(sse.contentAsString())));
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
