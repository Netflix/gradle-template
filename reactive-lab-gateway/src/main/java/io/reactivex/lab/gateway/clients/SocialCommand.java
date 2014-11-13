package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.SocialCommand.Social;
import io.reactivex.lab.gateway.clients.UserCommand.User;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        return RxNetty.createHttpClient("localhost", 9194, PipelineConfigurators.<ByteBuf> clientSseConfigurator())
                .submit(HttpClientRequest.createGet("/social?" + UrlGenerator.generate("userId", users)))
                .flatMap(r -> r.getContent().map(sse -> {
                    String social = sse.contentAsString();
                    System.out.println("social = " + social);
                    return Social.fromJson(social);
                }));
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
