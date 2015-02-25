package io.reactivex.lab.gateway.clients;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.reactivex.lab.gateway.routes.mock.BackendResponse;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

public class MockServiceCommand extends HystrixObservableCommand<BackendResponse> {

    private final long id;
    private final int numItems;
    private final int itemSize;
    private final int delay;

    public MockServiceCommand(long id, int numItems, int itemSize, int delay) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("MiddleTier"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("MiddleTier"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationSemaphoreMaxConcurrentRequests(5)
                        .withExecutionTimeoutInMilliseconds(200))); // change this timeout to <= 80 to see fallbacks
        this.id = id;
        this.numItems = numItems;
        this.itemSize = itemSize;
        this.delay = delay;
    }

    @Override
    protected Observable<BackendResponse> construct() {
        return RxNetty.createHttpClient("localhost", 9100)
                .submit(HttpClientRequest.createGet("/mock.json?numItems=" + numItems + "&itemSize=" + itemSize + "&delay=" + delay + "&id=" + id))
                .flatMap((HttpClientResponse<ByteBuf> r) -> r.getContent().map(b -> BackendResponse.fromJson(new ByteBufInputStream(b))));
    }

    @Override
    protected Observable<BackendResponse> resumeWithFallback() {
        return Observable.just(new BackendResponse(0, delay, numItems, itemSize, new String[] {}));
    }
}
