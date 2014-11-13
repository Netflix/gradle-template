package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.GeoCommand.GeoIP;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import rx.Observable;

import java.util.List;
import java.util.Map;

public class GeoCommand extends HystrixObservableCommand<GeoIP> {

    private final List<String> ips;

    public GeoCommand(List<String> ips) {
        super(HystrixCommandGroupKey.Factory.asKey("GeoIP"));
        this.ips = ips;
    }

    @Override
    protected Observable<GeoIP> run() {
        return RxNetty.createHttpClient("localhost", 9191, PipelineConfigurators.<ByteBuf>clientSseConfigurator())
                .submit(HttpClientRequest.createGet("/geo?" + UrlGenerator.generate("ip", ips)))
                .flatMap(r -> r.getContent().map(sse -> GeoIP.fromJson(sse.contentAsString())));
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
