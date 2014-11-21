package io.reactivex.lab.services;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.lab.services.common.SimpleJson;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.file.ClassPathFileRequestHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import rx.Observable;
import rx.schedulers.Schedulers;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.client.resolver.ServerResolvers;
import com.netflix.eureka2.interests.Interests;
import com.netflix.eureka2.registry.NetworkAddress.ProtocolType;
import com.netflix.eureka2.registry.ServicePort;
import com.netflix.eureka2.registry.datacenter.LocalDataCenterInfo;
import com.netflix.eureka2.server.EurekaWriteServer;
import com.netflix.eureka2.server.WriteServerConfig;
import com.netflix.eureka2.transport.EurekaTransports.Codec;

/**
 * Eureka discovery server for services and gateway to use for instance discovery.
 */
public class StartEurekaServer {

    /* properties for overriding config */
    public static final String EUREKA_SERVER_HOST = System.getProperty("reactivelab.eureka.server.host", "127.0.0.1");
    public static final Integer EUREKA_SERVER_READ_PORT = Integer.getInteger("reactivelab.eureka.server.read.port", 7001);
    public static final Integer EUREKA_SERVER_WRITE_PORT = Integer.getInteger("reactivelab.eureka.server.write.port", 7002);

    public static void main(String[] args) throws Exception {
        /* configure read/write Eureka server */
        System.setProperty("eureka2.eviction.allowedPercentage", "100"); // turn off eviction protection so during demos we don't hold on to hosts
        System.setProperty("eureka2.eviction.timeoutMillis", "4000"); // set far lower than normal for demo/playground purposes
        System.setProperty("eureka2.registration.heartbeat.intervalMillis", "3000"); // set lower for demo/playground purposes
        WriteServerConfig.WriteServerConfigBuilder builder = new WriteServerConfig.WriteServerConfigBuilder();
        builder.withReadServerPort(EUREKA_SERVER_READ_PORT).withWriteServerPort(EUREKA_SERVER_WRITE_PORT)
                .withWriteClusterAddresses(new String[] { EUREKA_SERVER_HOST })
                .withCodec(Codec.Avro)
                .withDataCenterType(LocalDataCenterInfo.DataCenterType.Basic);
        EurekaWriteServer eurekaWriteServer = new EurekaWriteServer(builder.build());

        /* start the server */
        eurekaWriteServer.start();

        EurekaClient client = Eureka.newClientBuilder(ServerResolvers.from(new ServerResolver.Server(EUREKA_SERVER_HOST, EUREKA_SERVER_READ_PORT))).build();
        /**
         * Background query for logging all events.
         */
        client.forInterest(Interests.forFullRegistry())
                .subscribeOn(Schedulers.computation())
                .forEach(n -> {
                    String vip = n.getData().getVipAddress();
                    String app = n.getData().getApp();
                    if (app == null) {
                        app = "none";
                    }
                    String name = n.getData().getDataCenterInfo().getName();
                    String id = n.getData().getId();

                    String ipAddress = n.getData().getDataCenterInfo()
                            .getAddresses().stream()
                            .filter(na -> na.getProtocolType() == ProtocolType.IPv4)
                            .collect(Collectors.toList()).get(0).getIpAddress();
                    HashSet<ServicePort> servicePorts = n.getData().getPorts();
                    int port = servicePorts.iterator().next().getPort();

                    System.out.println("Eureka => " + n.getKind() + " => App: " + app + " VIP: " + vip + " Name: " + name + " IP: " + ipAddress + " Port: " + port + " ID: " + id);
                });

        startEurekaDashboard(8888, client);
        eurekaWriteServer.waitTillShutdown();
    }

    public static class StaticFileHandler extends ClassPathFileRequestHandler {
        public StaticFileHandler() {
            super(".");
        }
    }

    private static void startEurekaDashboard(final int port, EurekaClient client) {
        final StaticFileHandler staticFileHandler = new StaticFileHandler();

        RxNetty.createHttpServer(port, (request, response) -> {
            if (request.getUri().startsWith("/dashboard")) {
                return staticFileHandler.handle(request, response);
            } else if (request.getUri().startsWith("/data")) {
                response.getHeaders().set(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream");
                response.getHeaders().set(HttpHeaders.Names.CACHE_CONTROL, "no-cache");
                return client.forInterest(Interests.forFullRegistry())
                        .flatMap(notification -> {
                            ByteBuf data = response.getAllocator().buffer();
                            data.writeBytes("data: ".getBytes());
                            Map<String, String> dataAttributes = new HashMap<>();
                            dataAttributes.put("type", notification.getKind().toString());
                            dataAttributes.put("instance-id", notification.getData().getId());
                            dataAttributes.put("vip", notification.getData().getVipAddress());
                            if (notification.getData().getStatus() != null) {
                                dataAttributes.put("status", notification.getData().getStatus().name());
                            }
                            HashSet<ServicePort> servicePorts = notification.getData().getPorts();
                            int port1 = servicePorts.iterator().next().getPort();
                            dataAttributes.put("port", String.valueOf(port1));
                            String jsonData = SimpleJson.mapToJson(dataAttributes);
                            data.writeBytes(jsonData.getBytes());
                            data.writeBytes("\n\n".getBytes());
                            return response.writeBytesAndFlush(data);
                        });
            } else {
                response.setStatus(HttpResponseStatus.NOT_FOUND);
                return Observable.empty();
            }
        }).start();
    }
}
