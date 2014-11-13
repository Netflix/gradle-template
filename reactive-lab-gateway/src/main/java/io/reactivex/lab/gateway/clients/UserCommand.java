package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.List;
import java.util.Map;

public class UserCommand extends HystrixObservableCommand<User> {

    private final List<String> userIds;
    private final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer;

    public UserCommand(List<String> userIds, LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer) {
        super(HystrixCommandGroupKey.Factory.asKey("User"));
        this.userIds = userIds;
        this.loadBalancer = loadBalancer;
    }

    @Override
    protected Observable<User> run() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/user?" + UrlGenerator.generate("userId",
                                                                                                         userIds));
        return loadBalancer.choose().map(holder -> holder.getClient())
                           .flatMap(client -> client.submit(request)
                                                    .flatMap(r -> r.getContent().map(
                                                            sse -> {
                                                                String user = sse.contentAsString();
                                                                System.out.println("user = " + user);
                                                                return User.fromJson(user);
                                                            })));
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
