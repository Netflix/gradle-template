package io.reactivex.lab.edge.clients;

import io.reactivex.lab.edge.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.edge.clients.PersonalizedCatalogCommand.Video;
import io.reactivex.lab.edge.common.RxNettySSE;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class BookmarksListCommand extends HystrixObservableCommand<List<Bookmark>> {

    final List<Video> videos;
    final String cacheKey;

    public BookmarksListCommand(List<Video> videos) {
        super(HystrixCommandGroupKey.Factory.asKey("GetBookmarks"));
        this.videos = videos;
        StringBuffer b = new StringBuffer();
        for (Video v : videos) {
            b.append(v.getId()).append("-");
        }
        this.cacheKey = b.toString();
    }

    @Override
    protected Observable<List<Bookmark>> run() {
        return RxNettySSE.createHttpClient("localhost", 9190)
                .submit(HttpClientRequest.createGet("/bookmarks?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> {
                    Observable<Bookmark> bytesToJson = r.getContent().map(sse -> {
                        return Bookmark.fromJson(sse.getEventData());
                    });
                    return bytesToJson;
                }).toList();
    }

    protected Observable<List<Bookmark>> getFallback() {
        List<Bookmark> bs = new ArrayList<>();
        for (Video v : videos) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("position", 0);
            data.put("videoId", v.getId());
            bs.add(new Bookmark(data));
        }
        return Observable.from(bs).toList();
    }

    @Override
    protected String getCacheKey() {
        return cacheKey;
    }
}
