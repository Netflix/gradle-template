package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookmarksCommand extends HystrixObservableCommand<Bookmark> {

    final List<Video> videos;
    final String cacheKey;

    public BookmarksCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("GetBookmarks"));
        this.videos = videos;
        StringBuilder b = new StringBuilder();
        for (Video v : videos) {
            b.append(v.getId()).append("-");
        }
        this.cacheKey = b.toString();
    }

    @Override
    protected Observable<Bookmark> run() {
        return RxNetty.createHttpClient("localhost", 9190, PipelineConfigurators.<ByteBuf>clientSseConfigurator())
                .submit(HttpClientRequest.createGet("/bookmarks?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> r.getContent().map(sse -> Bookmark.fromJson(sse.contentAsString())));
    }

    protected Observable<Bookmark> getFallback() {
        List<Bookmark> bs = new ArrayList<>();
        for (Video v : videos) {
            Map<String, Object> data = new HashMap<>();
            data.put("position", 0);
            data.put("videoId", v.getId());
            bs.add(new Bookmark(data));
        }
        return Observable.from(bs);
    }

    @Override
    protected String getCacheKey() {
        return cacheKey;
    }

    public static class Bookmark {

        private final Map<String, Object> data;

        Bookmark(Map<String, Object> data) {
            this.data = data;
        }

        public static Bookmark fromJson(String json) {
            return new Bookmark(SimpleJson.jsonToMap(json));
        }

        public int getPosition() {
            return (int) data.get("position");
        }

        public int getVideoId() {
            return Integer.parseInt(String.valueOf(data.get("videoId")));
        }

    }
}
