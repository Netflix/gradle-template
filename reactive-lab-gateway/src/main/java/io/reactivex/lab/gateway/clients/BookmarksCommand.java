package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookmarksCommand extends HystrixObservableCommand<Bookmark> {

    final List<Video> videos;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer;
    final String cacheKey;

    public BookmarksCommand(List<Video> videos, LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer) {
        super(HystrixCommandGroupKey.Factory.asKey("GetBookmarks"));
        this.videos = videos;
        this.loadBalancer = loadBalancer;
        StringBuilder b = new StringBuilder();
        for (Video v : videos) {
            b.append(v.getId()).append("-");
        }
        this.cacheKey = b.toString();
    }

    @Override
    public Observable<Bookmark> construct() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/bookmarks?" + UrlGenerator.generate("videoId", videos));
        return loadBalancer.choose()
                .map(holder -> holder.getClient())
                .<Bookmark>flatMap(client -> client.submit(request)
                                         .flatMap(r -> r.getContent().map((ServerSentEvent sse) -> Bookmark.fromJson(sse.contentAsString()))))
                .retry(1);
    }

    protected Observable<Bookmark> resumeWithFallback() {
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
