package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Catalog;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        return RxNetty.createHttpClient("localhost", 9192, PipelineConfigurators.<ByteBuf>clientSseConfigurator())
                .submit(HttpClientRequest.createGet("/catalog?" + UrlGenerator.generate("userId", users)))
                .flatMap(r -> r.getContent().map(sse -> {
                    String catalog = sse.contentAsString();
                    System.out.println("catalog = " + catalog);
                    return Catalog.fromJson(catalog);
                }));
    }

    public static class Catalog {

        private final Map<String, Object> data;

        private Catalog(Map<String, Object> data) {
            this.data = data;
        }

        @SuppressWarnings("unchecked")
        public Observable<Video> videos() {
            try {
                return Observable.from((List<Integer>) data.get("videos")).map(Video::new);
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
