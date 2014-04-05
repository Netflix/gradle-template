package io.reactivex.lab.edge.hystrix;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.reactivex.lab.edge.common.BackendResponse;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;

public class MiddleTierClientHystrixCommand extends HystrixObservableCommand<BackendResponse> {

    private final long id;
    private final int numItems;
    private final int itemSize;
    private final int delay;

    protected MiddleTierClientHystrixCommand(long id, int numItems, int itemSize, int delay) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("MiddleTier"))
                .andCommandKey(HystrixCommandKey.Factory.asKey("MiddleTier"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationSemaphoreMaxConcurrentRequests(5)
                        .withExecutionIsolationThreadTimeoutInMilliseconds(200))); // change this timeout to <= 80 to see fallbacks
        this.id = id;
        this.numItems = numItems;
        this.itemSize = itemSize;
        this.delay = delay;
    }

    @Override
    protected Observable<BackendResponse> run() {
        return RxNetty.createHttpClient("localhost", 9090)
                .submit(HttpClientRequest.createGet("/mock.json?numItems=" + numItems + "&itemSize=" + itemSize + "&delay=" + delay + "&id=" + id))
                .flatMap((HttpClientResponse<ByteBuf> r) -> {
                    Observable<BackendResponse> bytesToJson = r.getContent().map(b -> {
                        return BackendResponse.fromJson(new ByteBufInputStream(b));
                    });
                    return bytesToJson;
                });
    }

    protected Observable<BackendResponse> getFallback() {
        return Observable.just(new BackendResponse(0, delay, numItems, itemSize, new String[] {}));
    }
}
