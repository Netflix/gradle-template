package io.reactivex.lab.tutorial;

import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.interests.Interests;
import com.netflix.eureka2.registry.NetworkAddress;
import com.netflix.eureka2.registry.ServicePort;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import io.reactivex.netty.protocol.http.server.HttpServer;
import netflix.ocelli.Host;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.LoadBalancers;
import netflix.ocelli.MembershipEvent;
import netflix.ocelli.eureka.EurekaMembershipSource;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This example builds over the {@link ClientServerWithDiscovery} example by adding the load balancer Ocelli.
 * So, in comparison to {@link ClientServerWithDiscovery} which directly fetches the server instance information from
 * eureka, this example chooses an optimal host from the load balancer.
 *
 * In order to be a standalone example, this also starts an embedded eureka server.
 */
public class ClientServerWithLoadBalancer {

    public static void main(String[] args) throws Exception {

        final int eurekaReadServerPort = 7007;
        final int eurekaWriteServerPort = 7008;

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
        Observable<MembershipEvent<Host>> eurekaHostSource = createEurekaHostStream(eurekaClient, vipAddress);

        /**
         * Instead of directly using the host and port from eureka as in example {@link ClientServerWithDiscovery},
         * choose a host from the load balancer.
         */
        createRequestFromLB(eurekaHostSource)
                /* Block till you get the response. In a real world application, one should not be blocked but chained
                 * into a response to the caller. */
                .toBlocking()
                /**
                 * Print each content of the response.
                 */
                .forEach(System.out::println);
    }

    public static Observable<String> createRequestFromLB(Observable<MembershipEvent<Host>> eurekaHostSource) {

        /**
         * Create a LoadBalancer instance from eureka's host stream.
         */
        return LoadBalancers.fromHostSource(
                /**
                 * Map over the host event and create a RxNetty HTTP client.
                 * This enable Ocelli to directly manage the client instance and hence reduce a map lookup to get a
                 * client instance for a host for every request processing.
                 * If you prefer Ocelli to manage the host instances, that is possible too by omitting this map function
                 * and transforming the retrieved host via {@link LoadBalancer#choose()} to a client of
                 * your choice.
                 */
                eurekaHostSource.map(
                        hostEvent -> {
                            Host host = hostEvent.getClient();
                            /**
                             * Typically, you will use a pool of clients, so that for the same host, you do not end up creating a
                             * new client instance.
                             * This example creates a new client to reduce verbosity.
                             */
                            HttpClient<ByteBuf, ByteBuf> client = RxNetty.createHttpClient(host.getHostName(),
                                                                                           host.getPort());

                            /**
                             * Since, Ocelli expects a {@link netflix.ocelli.MembershipEvent} instance, wrap the client
                             * into that event.
                             */
                            return new MembershipEvent<>(hostEvent.getType(), client);
                        }))
                .choose() /* Chooses the best possible host for the next request*/
                /**
                 * Submit the request to the returned client.
                 */
                .flatMap(client ->
                                 client.submit(HttpClientRequest.createGet("/"))
                                         /* Print the HTTP initial line and headers. Return the content.*/
                                         .flatMap((Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>) response -> {
                                                     /**
                                                      * Printing the HTTP initial line.
                                                      */
                                                     System.out.println( response.getHttpVersion().text() + ' '
                                                                         + response.getStatus().code() + ' '
                                                                         + response .getStatus().reasonPhrase());
                                                     /**
                                                      * Printing HTTP headers.
                                                      */
                                                     for (Map.Entry<String, String> header : response.getHeaders().entries()) {
                                                         System.out.println(header.getKey() + ": " + header.getValue());
                                                     }

                                                     // Line break after the headers.
                                                     System.out.println();

                                                     return response.getContent();
                                                 })
                                         /* Convert the ByteBuf for each content chunk into a string. */
                                         .map(byteBuf -> byteBuf.toString(Charset.defaultCharset())));
    }

    public static Observable<MembershipEvent<Host>> createEurekaHostStream(EurekaClient eurekaClient,
                                                                           String vipAddress) {

        EurekaMembershipSource membershipSource = new EurekaMembershipSource(eurekaClient);

        /**
         * Eureka client caches data streams from the server to decouple users from blips in connection between client
         * & server. The below call makes sure that the data is fetched by the client and hence it is guaranteed to be
         * available when the load balancer queries for the same.
         *
         * This is just to make sure that the demo runs without inducing artificial delays in the code.
         * A real application should not advertise itself healthy in eureka till it has all the information it requires
         * in order to run gracefully. In this case, the list of target servers from eureka.
         */
        eurekaClient.forInterest(Interests.forFullRegistry()).take(1).toBlocking().first();// Warm up eureka data

        return membershipSource.forInterest(Interests.forVips(vipAddress), instanceInfo -> {
            /**
             * Filtering out all non-IPv4 addresses.
             */
            String ipAddress = instanceInfo.getDataCenterInfo()
                                           .getAddresses().stream()
                                           .filter(na -> na.getProtocolType() == NetworkAddress.ProtocolType.IPv4)
                                           .collect(Collectors.toList()).get(0).getIpAddress();
            HashSet<ServicePort> servicePorts = instanceInfo.getPorts();
            ServicePort portToUse = servicePorts.iterator().next();
            return new Host(ipAddress, portToUse.getPort());
        });
    }
}
