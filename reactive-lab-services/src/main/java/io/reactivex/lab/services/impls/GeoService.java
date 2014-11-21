package io.reactivex.lab.services.impls;

import com.netflix.eureka2.client.EurekaClient;

import io.reactivex.lab.services.common.SimpleJson;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GeoService extends AbstractMiddleTierService {

    public GeoService(EurekaClient client) {
        super("reactive-lab-geo-service", client);
    }

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response) {
        return request.getContent().flatMap(i -> {
            List<String> ips = request.getQueryParameters().get("ip");
            Map<String, Object> data = new HashMap<>();
            for (String ip : ips) {
                Map<String, Object> ip_data = new HashMap<>();
                ip_data.put("country_code", "GB");
                ip_data.put("longitude", "-0.13");
                ip_data.put("latitude", "51.5");
                data.put(ip, ip_data);
            }
            return response.writeStringAndFlush("data: " + SimpleJson.mapToJson(data) + "\n")
                    .doOnCompleted(response::close);
        }).delay(10, TimeUnit.MILLISECONDS);
    }
}
