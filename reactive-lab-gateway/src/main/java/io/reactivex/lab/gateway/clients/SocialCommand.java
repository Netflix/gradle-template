package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.SocialCommand.Social;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.lab.gateway.loadbalancer.DiscoveryAndLoadBalancer;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SocialCommand extends HystrixObservableCommand<Social> {

    private final List<User> users;
    private static final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer =
            DiscoveryAndLoadBalancer.getFactory().forVip("reactive-lab-social-service");

    public SocialCommand(User user) {
        this(Arrays.asList(user));
        // replace with HystrixCollapser
    }

    public SocialCommand(List<User> users) {
        super(HystrixCommandGroupKey.Factory.asKey("Social"));
        this.users = users;
    }

    @Override
    protected Observable<Social> construct() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/social?" + UrlGenerator.generate("userId", users));
        return loadBalancer.choose().map(holder -> holder.getClient())
                .<Social>flatMap(client -> client.submit(request)
                                         .flatMap(r -> r.getContent().map((ServerSentEvent sse) -> {
                                             String social = sse.contentAsString();
                                             return Social.fromJson(social);
                                         })))
                .retry(1);
    }
    
    @Override
    protected Observable<Social> resumeWithFallback() {
        Map<String, Object> user = new HashMap<>();
        user.put("userId", users.get(0).getId());
        user.put("friends", Collections.emptyList());
        
        return Observable.just(new Social(user));
    }
    
    private static int randomUser() {
        return ((int) (Math.random() * 10000));
    }

    public static class Social {

        private final Map<String, Object> data;

        private Social(Map<String, Object> data) {
            this.data = data;
        }

        public Map<String, Object> getDataAsMap() {
            return data;
        }

        public static Social fromJson(String json) {
            return new Social(SimpleJson.jsonToMap(json));
        }

    }

}
