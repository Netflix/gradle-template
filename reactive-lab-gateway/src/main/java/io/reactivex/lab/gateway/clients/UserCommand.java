package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.lab.gateway.loadbalancer.DiscoveryAndLoadBalancer;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.List;
import java.util.Map;

public class UserCommand extends HystrixObservableCommand<User> {

    private final List<String> userIds;
    private static final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer =
            DiscoveryAndLoadBalancer.getFactory().forVip("reactive-lab-user-service");

    public UserCommand(List<String> userIds) {
        super(HystrixCommandGroupKey.Factory.asKey("User"));
        this.userIds = userIds;
    }

    @Override
    protected Observable<User> run() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/user?" + UrlGenerator.generate("userId", userIds));
        return loadBalancer.choose().map(holder -> holder.getClient())
                .<User>flatMap(client -> client.submit(request)
                                         .flatMap(r -> r.getContent().map(
                                                 (ServerSentEvent sse) -> {
                                                     String user = sse.contentAsString();
                                                     return User.fromJson(user);
                                                 })))
                .retry(1);
    }

    public static class User implements ID {
        private final Map<String, Object> data;

        public User(Map<String, Object> jsonToMap) {
            this.data = jsonToMap;
        }

        public static User fromJson(String json) {
            return new User(SimpleJson.jsonToMap(json));
        }

        public int getId() {
            return Integer.parseInt(String.valueOf(data.get("userId")));
        }

        public String getName() {
            return (String) data.get("name");
        }

    }

}
