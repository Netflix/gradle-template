package io.reactivex.lab.edge.clients;

import io.reactivex.lab.edge.clients.PersonalizedCatalogCommand.Catalog;
import io.reactivex.lab.edge.clients.UserCommand.User;
import io.reactivex.lab.edge.common.RxNettySSE;
import io.reactivex.lab.edge.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class PersonalizedCatalogCommand extends HystrixObservableCommand<Catalog> {

    private final List<User> users;

    public PersonalizedCatalogCommand(User user) {
        this(Arrays.asList(user));
        // replace with HystrixCollapser
    }

    public PersonalizedCatalogCommand(List<User> users) {
        super(HystrixCommandGroupKey.Factory.asKey("PersonalizedCatalog"));
        this.users = users;
    }

    @Override
    protected Observable<Catalog> run() {
        return RxNettySSE.createHttpClient("localhost", 9192)
                .submit(HttpClientRequest.createGet("/catalog?" + UrlGenerator.generate("userId", users)))
                .flatMap(r -> {
                    Observable<Catalog> bytesToJson = r.getContent().map(sse -> {
                        return Catalog.fromJson(sse.getEventData());
                    });
                    return bytesToJson;
                });
    }

    public static class Catalog {

        private final Map<String, Object> data;

        private Catalog(Map<String, Object> data) {
            this.data = data;
        }

        @SuppressWarnings("unchecked")
        public Observable<Video> videos() {
            try {
                return Observable.from((List<Integer>) data.get("videos")).map(i -> new Video(i));
            } catch (Exception e) {
                return Observable.error(e);
            }
        }

        public static Catalog fromJson(String json) {
            return new Catalog(SimpleJson.jsonToMap(json));
        }

    }

    public static class Video implements ID {

        private final int id;

        public Video(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

    }

}
