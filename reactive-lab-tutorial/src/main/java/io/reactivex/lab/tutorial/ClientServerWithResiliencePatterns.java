package io.reactivex.lab.tutorial;

import com.netflix.eureka2.client.EurekaClient;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import netflix.ocelli.Host;
import netflix.ocelli.MembershipEvent;
import rx.Observable;

import java.util.concurrent.TimeUnit;

/**
 * This example builds over the {@link ClientServerWithLoadBalancer} example by adding hystrix.
 * So, in comparison to {@link ClientServerWithLoadBalancer} which directly invokes the load balancer, this example
 * wraps the invocation and request execution with a hystrix command.
 *
 * In order to be a standalone example, this also starts an embedded eureka server.
 */
public class ClientServerWithResiliencePatterns {

    public static void main(String[] args) throws Exception {

        final int eurekaReadServerPort = 7008;
        final int eurekaWriteServerPort = 7010;

        /**
         * Starts an embedded eureka server with the defined read and write ports.
         */
        ClientServerWithDiscovery.startEurekaServer(eurekaReadServerPort, eurekaWriteServerPort);

        /**
         * Create eureka client with the same read and write ports for the embedded eureka server.
         */
        EurekaClient eurekaClient = ClientServerWithDiscovery.createEurekaClient(eurekaReadServerPort,
                                                                                 eurekaWriteServerPort);

        /**
         * Reuse {@link ClientServer} example to start an RxNetty server on the passed port.
         */
        HttpServer<ByteBuf, ByteBuf> server = ClientServer.startServer(8089);

        /**
         * Register the server started above with eureka using a unique virtual IP address (VIP).
         * Eureka uses VIPs to group homogeneous instances of a service together, so that they can be used by clients,
         * interchangeably.
         */
        String vipAddress = "mock_server-" + server.getServerPort();
        ClientServerWithDiscovery.registerWithEureka(server.getServerPort(), eurekaClient, vipAddress);

        /**
         * Using the eureka client, create an Ocelli Host event stream.
         * Ocelli, uses this host stream to know about the available hosts.
         */
        Observable<MembershipEvent<Host>> eurekaHostSource = ClientServerWithLoadBalancer.createEurekaHostStream(
                eurekaClient, vipAddress);

        MyCommand myCommand = new MyCommand(eurekaHostSource);

        /**
         * This executes the request on the client (just as {@link ClientServerWithLoadBalancer} but using hystrix.
         */
        myCommand.toObservable()
                /* Block till you get the response. In a real world application, one should not be blocked but chained
                 * into a response to the caller. */
                .toBlocking()
                /**
                 * Print each content of the response.
                 */
                .forEach(System.out::println);

    }

    public static class MyCommand extends HystrixObservableCommand<String> {

        private final Observable<MembershipEvent<Host>> eurekaHostSource;

        public MyCommand(Observable<MembershipEvent<Host>> eurekaHostSource) {
            super(HystrixCommandGroupKey.Factory.asKey("MyCommand"));
            this.eurekaHostSource = eurekaHostSource;
        }

        @Override
        protected Observable<String> construct() {
            return ClientServerWithLoadBalancer.createRequestFromLB(eurekaHostSource)
                                                /**
                                                 * Artificial delay to demonstrate hystrix timeouts and fallbacks.
                                                 * Hystrix default timeout is 1 second.
                                                 */
                                               .delay(1, TimeUnit.SECONDS);
        }

        @Override
        protected Observable<String> resumeWithFallback() {
            return Observable.just("Fallback from Hystrix.");
        }
    }
}
