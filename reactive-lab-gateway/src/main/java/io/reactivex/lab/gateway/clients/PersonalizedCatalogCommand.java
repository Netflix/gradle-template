package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.PersonalizedCatalogCommand.Catalog;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.lab.gateway.loadbalancer.DiscoveryAndLoadBalancer;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected Observable<Catalog> construct() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/catalog?" + UrlGenerator.generate("userId", users));
        return loadBalancer.choose()
                           .map(holder -> holder.getClient())
                           .<Catalog>flatMap(client -> client.submit(request)
                                                    .flatMap(r -> r.getContent()
                                                                   .map((ServerSentEvent sse) -> Catalog.fromJson(sse.contentAsString()))))
                           .retry(1);
    }
    
    @Override
    protected Observable<Catalog> resumeWithFallback() {
        return Observable.from(users).<Catalog>map(u -> {
            try {
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", u.getId());

                userData.put("list_title", "Really quirky and over detailed list title!");
                userData.put("other_data", "goes_here");
                userData.put("videos", Arrays.asList(12345, 23456, 34567, 45678, 56789, 67890));
                return new Catalog(userData);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
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
