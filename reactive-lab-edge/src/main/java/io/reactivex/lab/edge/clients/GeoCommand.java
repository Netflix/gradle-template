package io.reactivex.lab.edge.clients;

import io.reactivex.lab.edge.clients.GeoCommand.GeoIP;
import io.reactivex.lab.edge.common.RxNettySSE;
import io.reactivex.lab.edge.common.SimpleJson;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;

import java.util.List;
import java.util.Map;

import rx.Observable;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;

public class GeoCommand extends HystrixObservableCommand<GeoIP> {

    private final List<String> ips;

    public GeoCommand(List<String> ips) {
        super(HystrixCommandGroupKey.Factory.asKey("GeoIP"));
        this.ips = ips;
    }

    @Override
    protected Observable<GeoIP> run() {
        return RxNettySSE.createHttpClient("localhost", 9191)
                .submit(HttpClientRequest.createGet("/geo?" + UrlGenerator.generate("ip", ips)))
                .flatMap(r -> {
                    Observable<GeoIP> bytesToJson = r.getContent().map(sse -> {
                        return GeoIP.fromJson(sse.getEventData());
                    });
                    return bytesToJson;
                });
    }

    public static class GeoIP {

        private final Map<String, Object> data;

        private GeoIP(Map<String, Object> data) {
            this.data = data;
        }

        public static GeoIP fromJson(String json) {
            return new GeoIP(SimpleJson.jsonToMap(json));
        }

    }
}
