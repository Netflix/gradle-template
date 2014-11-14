package io.reactivex.lab.gateway.loadbalancer;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.client.resolver.ServerResolvers;
import com.netflix.eureka2.interests.Interests;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.eureka.EurekaMembershipSource;
import netflix.ocelli.rxnetty.HttpClientPool;

import static io.reactivex.netty.pipeline.PipelineConfigurators.clientSseConfigurator;

/**
 * Lifecycle of LoadBalancer for app and static accessor.
 * <p>
 * This class is currently a hack. Upgrade this later to use DI of some kind ... Dagger2?
 */
public class DiscoveryAndLoadBalancer {

    public static final String EUREKA_SERVER_HOST = System.getProperty("reactivelab.eureka.server.host", "127.0.0.1");
    public static final Integer EUREKA_SERVER_READ_PORT = Integer.getInteger("reactivelab.eureka.server.read.port", 7001);
    public static final Integer EUREKA_SERVER_WRITE_PORT = Integer.getInteger("reactivelab.eureka.server.write.port", 7002);

    public static LoadBalancerFactory getFactory() {
        return Initializer.factory;
    }

    private static class Initializer {
        private static LoadBalancerFactory _lb;
        static {
            ServerResolver.Server discoveryServer = new ServerResolver.Server(EUREKA_SERVER_HOST, EUREKA_SERVER_READ_PORT);
            ServerResolver.Server registrationServer = new ServerResolver.Server(EUREKA_SERVER_HOST, EUREKA_SERVER_WRITE_PORT);
            EurekaClient client = Eureka.newClientBuilder(ServerResolvers.from(discoveryServer),
                    ServerResolvers.from(registrationServer))
                    .build();

            EurekaMembershipSource membershipSource = new EurekaMembershipSource(client);

            _lb = new LoadBalancerFactory(membershipSource,
                    new HttpClientPool<ByteBuf, ServerSentEvent>(clientSseConfigurator()));

            client.forInterest(Interests.forFullRegistry()).forEach(System.out::println);
            
            System.out.println("------------ DONE");
            /**
             * This is making sure that eureka's client registry is warmed up.
             */
            client.forInterest(Interests.forFullRegistry()).take(1).toBlocking().single();
        }
        static final LoadBalancerFactory factory = _lb;
    }
}
