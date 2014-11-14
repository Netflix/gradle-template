package io.reactivex.lab.gateway.clients;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Catalog;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.lab.gateway.loadbalancer.DiscoveryAndLoadBalancer;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class PersonalizedCatalogCommand extends HystrixObservableCommand<Catalog> {

    private final List<User> users;
    private static final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer =
            DiscoveryAndLoadBalancer.getFactory().forVip("reactive-lab-personalized-catalog-service");

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
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/catalog?" + UrlGenerator.generate("userId", users));
        return loadBalancer.choose().map(holder -> holder.getClient())
                .flatMap(client -> client.submit(request)
                        .flatMap(r -> r.getContent().map(sse -> {
                            String catalog = sse.contentAsString();
                            return Catalog.fromJson(catalog);
                        })));
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
