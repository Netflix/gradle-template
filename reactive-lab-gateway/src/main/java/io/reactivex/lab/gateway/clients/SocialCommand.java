package io.reactivex.lab.gateway.clients;

import io.reactivex.lab.gateway.clients.SocialCommand.Social;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.RxNettySSE;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class SocialCommand extends HystrixObservableCommand<Social> {

    private final List<User> users;

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
        return RxNettySSE.createHttpClient("localhost", 9194)
                .submit(HttpClientRequest.createGet("/social?" + UrlGenerator.generate("userId", users)))
                .flatMap(r -> {
                    Observable<Social> bytesToJson = r.getContent().map(sse -> {
                        return Social.fromJson(sse.contentAsString());
                    });
                    return bytesToJson;
                });
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
