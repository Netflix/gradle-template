package io.reactivex.lab.gateway.clients;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.lab.gateway.clients.GeoCommand.GeoIP;
import io.reactivex.lab.gateway.common.SimpleJson;
import io.reactivex.lab.gateway.loadbalancer.DiscoveryAndLoadBalancer;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.rxnetty.HttpClientHolder;
import rx.Observable;

import java.util.List;
import java.util.Map;

public class GeoCommand extends HystrixObservableCommand<GeoIP> {

    private final List<String> ips;
    private static final LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> loadBalancer =
            DiscoveryAndLoadBalancer.getFactory().forVip("reactive-lab-geo-service");

    public GeoCommand(List<String> ips) {
        super(HystrixCommandGroupKey.Factory.asKey("GeoIP"));
        this.ips = ips;
    }

    @Override
    protected Observable<GeoIP> construct() {
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/geo?" + UrlGenerator.generate("ip", ips));
        return loadBalancer.choose()
                           .map(holder -> holder.getClient())
                           .<GeoIP>flatMap(client -> client.submit(request)
                                                    .flatMap(r -> r.getContent()
                                                                   .map((ServerSentEvent sse) -> GeoIP.fromJson(sse.contentAsString()))))
                           .retry(1);
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
