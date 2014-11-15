package io.reactivex.lab.services;

import java.util.HashSet;
import java.util.stream.Collectors;

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

                    String ipAddress = n.getData().getDataCenterInfo()
                            .getAddresses().stream()
                            .filter(na -> na.getProtocolType() == ProtocolType.IPv4)
                            .collect(Collectors.toList()).get(0).getIpAddress();
                    HashSet<ServicePort> servicePorts = n.getData().getPorts();
                    int port = servicePorts.iterator().next().getPort();

                    System.out.println("Eureka => " + n.getKind() + " => App: " + app + " VIP: " + vip + " Name: " + name + " IP: " + ipAddress + " Port: " + port);
                });

        eurekaWriteServer.waitTillShutdown();
    }
}
