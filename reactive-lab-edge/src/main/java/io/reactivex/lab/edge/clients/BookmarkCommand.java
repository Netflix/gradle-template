package io.reactivex.lab.edge.clients;

import io.reactivex.lab.edge.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.edge.clients.PersonalizedCatalogCommand.Video;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netflix.hystrix.HystrixCollapser.CollapsedRequest;
import com.netflix.hystrix.HystrixObservableCollapser;
import com.netflix.hystrix.HystrixObservableCommand;

public class BookmarkCommand extends HystrixObservableCollapser<List<Bookmark>, Bookmark, Video> {

    private final Video video;

    public BookmarkCommand(Video video) {
        this.video = video;
    }

    @Override
    public Video getRequestArgument() {
        return video;
    }

    @Override
    protected HystrixObservableCommand<List<Bookmark>> createCommand(Collection<CollapsedRequest<Bookmark, Video>> requests) {
        List<Video> videos = new ArrayList<>();
        for (CollapsedRequest<Bookmark, Video> r : requests) {
            videos.add(r.getArgument());
        }
        return new BookmarksListCommand(videos);
    }

    @Override
    protected void mapResponseToRequests(List<Bookmark> batchResponse, Collection<CollapsedRequest<Bookmark, Video>> requests) {
        Map<Integer, Bookmark> bs = new HashMap<>();
        for (Bookmark b : batchResponse) {
            bs.put(b.getVideoId(), b);
        }
        for (CollapsedRequest<Bookmark, Video> r : requests) {
            Bookmark b = bs.get(r.getArgument().getId());
            if (b == null) {
                // would normally send a default instead of failing here
                r.setException(new Exception("No bookmark"));
            } else {
                r.setResponse(b);
            }
        }
    }
}
