package io.reactivex.lab.services;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolvers;
import com.netflix.turbine.Turbine;
import com.netflix.turbine.discovery.eureka.EurekaStreamDiscovery;

public class StartTurbineServer {

    public static void main(String... args) {
        try {
            EurekaClient eurekaClient = Eureka.newClient(ServerResolvers.just(StartEurekaServer.EUREKA_SERVER_HOST, StartEurekaServer.EUREKA_SERVER_READ_PORT), null);
            Turbine.startServerSentEventServer(4444, EurekaStreamDiscovery.create("reactive-lab", "http://{HOSTNAME}/hystrix.stream", eurekaClient));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
