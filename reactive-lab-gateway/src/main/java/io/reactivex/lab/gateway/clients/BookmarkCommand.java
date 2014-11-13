package io.reactivex.lab.gateway.clients;

import io.reactivex.lab.gateway.clients.BookmarksCommand.Bookmark;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Video;

import java.util.*;

import rx.functions.Func1;

import com.netflix.hystrix.HystrixCollapser.CollapsedRequest;
import com.netflix.hystrix.*;

public class BookmarkCommand extends HystrixObservableCollapser<Integer, Bookmark, Bookmark, Video> {

    private final Video video;

    public BookmarkCommand(Video video) {
        this.video = video;
    }

    @Override
    public Video getRequestArgument() {
        return video;
    }

    @Override
    protected HystrixObservableCommand<Bookmark> createCommand(Collection<CollapsedRequest<Bookmark, Video>> requests) {
        List<Video> videos = new ArrayList<>();
        for (CollapsedRequest<Bookmark, Video> r : requests) {
            videos.add(r.getArgument());
        }
        return new BookmarksCommand(videos);
    }

    protected void onMissingResponse(CollapsedRequest<Bookmark, Video> r) {
        // set a default using setResponse or an exception like this
        r.setException(new Exception("No bookmark"));
    }

    @Override
    protected Func1<Bookmark, Integer> getBatchReturnTypeKeySelector() {
        return (Bookmark b) -> b.getVideoId();
    }

    @Override
    protected Func1<Video, Integer> getRequestArgumentKeySelector() {
        return (Video v) -> v.getId();
    }

    @Override
    protected Func1<Bookmark, Bookmark> getBatchReturnTypeToResponseTypeMapper() {
        return (b) -> b;
    }

}
