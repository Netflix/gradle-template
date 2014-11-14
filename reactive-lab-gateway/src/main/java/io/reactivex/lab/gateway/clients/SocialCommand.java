package io.reactivex.lab.gateway.clients;

import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.SocialCommand.Social;
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
    protected Observable<Social> run() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/social?" + UrlGenerator.generate("userId",
                users));
        return loadBalancer.choose().map(holder -> holder.getClient())
                .flatMap(client -> client.submit(request)
                        .flatMap(r -> r.getContent().map(sse -> {
                            String social = sse.contentAsString();
                            System.out.println("social = " + social);
                            return Social.fromJson(social);
                        })));
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
