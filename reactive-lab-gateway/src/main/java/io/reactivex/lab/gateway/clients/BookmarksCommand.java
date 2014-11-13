package io.reactivex.lab.gateway.clients;

import io.reactivex.lab.gateway.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.common.RxNettySSE;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class BookmarksCommand extends HystrixObservableCommand<Bookmark> {

    final List<Video> videos;
    final String cacheKey;

    public BookmarksCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("GetBookmarks"));
        this.videos = videos;
        StringBuffer b = new StringBuffer();
        for (Video v : videos) {
            b.append(v.getId()).append("-");
        }
        this.cacheKey = b.toString();
    }

    @Override
    protected Observable<Bookmark> run() {
        return RxNettySSE.createHttpClient("localhost", 9190)
                .submit(HttpClientRequest.createGet("/bookmarks?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> {
                    Observable<Bookmark> bytesToJson = r.getContent().map(sse -> {
                        return Bookmark.fromJson(sse.contentAsString());
                    });
                    return bytesToJson;
                });
    }

    protected Observable<Bookmark> getFallback() {
        List<Bookmark> bs = new ArrayList<>();
        for (Video v : videos) {
            Map<String, Object> data = new HashMap<String, Object>();
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
