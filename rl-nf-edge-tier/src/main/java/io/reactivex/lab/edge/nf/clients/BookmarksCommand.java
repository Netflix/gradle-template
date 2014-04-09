package io.reactivex.lab.edge.nf.clients;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.edge.common.RxNettySSE;
import io.reactivex.lab.edge.common.SimpleJson;
import io.reactivex.lab.edge.nf.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.edge.nf.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class BookmarksCommand extends HystrixObservableCommand<Bookmark> {

    final List<Video> videos;

    public BookmarksCommand(Video video) {
        this(Arrays.asList(video));
        // replace with HystrixCollapser
    }

    public BookmarksCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("GetBookmarks"));
        this.videos = videos;
    }

    @Override
    protected Observable<Bookmark> run() {
        return RxNettySSE.createHttpClient("localhost", 9190)
                .submit(HttpClientRequest.createGet("/bookmarks?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> {
                    Observable<Bookmark> bytesToJson = r.getContent().map(sse -> {
                        return Bookmark.fromJson(sse.getEventData());
                    });
                    return bytesToJson;
                });
    }

    public static class Bookmark {

        private final Map<String, Object> data;

        private Bookmark(Map<String, Object> data) {
            this.data = data;
        }

        public static Bookmark fromJson(String json) {
            return new Bookmark(SimpleJson.jsonToMap(json));
        }

        public int getPosition() {
            return (int) data.get("position");
        }

    }
}
