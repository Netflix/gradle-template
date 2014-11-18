package io.reactivex.lab.tutorial;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.client.resolver.ServerResolvers;
import com.netflix.eureka2.interests.ChangeNotification;
import com.netflix.eureka2.registry.InstanceInfo;
import com.netflix.eureka2.registry.NetworkAddress;
import com.netflix.eureka2.registry.ServicePort;
import com.netflix.eureka2.registry.datacenter.BasicDataCenterInfo;
import com.netflix.eureka2.registry.datacenter.LocalDataCenterInfo;
import com.netflix.eureka2.server.EurekaWriteServer;
import com.netflix.eureka2.server.WriteServerConfig;
import com.netflix.eureka2.transport.EurekaTransports;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * This example builds over the basic {@link ClientServer} example by adding registration to eureka server.
 *
 * In order to be a standalone example, this also starts an embedded eureka server.
 */
public class ClientServerWithDiscovery {

    public static class Host {

        private String ipAddress;
        private int port;

        public Host(String ipAddress, int port) {
            this.ipAddress = ipAddress;
            this.port = port;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public int getPort() {
            return port;
        }
    }

    public static void main(String[] args) throws Exception {

        final int eurekaReadServerPort = 7005;
        final int eurekaWriteServerPort = 7006;

        /**
         * Starts an embedded eureka server with the defined read and write ports.
         */
        startEurekaServer(eurekaReadServerPort, eurekaWriteServerPort);

        /**
         * Create eureka client with the same read and write ports for the embedded eureka server.
         */
        EurekaClient eurekaClient = createEurekaClient(eurekaReadServerPort, eurekaWriteServerPort);

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
        registerWithEureka(server.getServerPort(), eurekaClient, vipAddress);

        /**
         * Retrieve the instance information of the registered server from eureka.
         * This is to demonstrate how to use eureka to fetch information about any server in your deployment.
         * In order to fetch information from eureka, one MUST know the VIP address of the server before hand.
         */
        InstanceInfo serverInfo = getServerInfo(eurekaClient, vipAddress);

        /**
         * Retrieve IPAddress and port information from the instance information returned from eureka.
         */
        Host host = getServerHostAndPort(serverInfo);

        /**
         * Reuse {@link ClientServer} example to create an HTTP request to the server retrieved from eureka.
         */
        ClientServer.createRequest(host.getIpAddress(), host.getPort())
                    /* Block till you get the response. In a real world application, one should not be blocked but chained
                     * into a response to the caller. */
                    .toBlocking()
                    /**
                     * Print each content of the response.
                     */
                    .forEach(System.out::println);
    }

    public static Host getServerHostAndPort(InstanceInfo serverInfo) {
        String ipAddress = serverInfo.getDataCenterInfo()
                                       .getAddresses().stream()
                                       .filter(na -> na.getProtocolType() == NetworkAddress.ProtocolType.IPv4)
                                       .collect(Collectors.toList()).get(0).getIpAddress();

        Integer port = serverInfo.getPorts().iterator().next().getPort();

        return new Host(ipAddress, port);
    }

    public static InstanceInfo getServerInfo(EurekaClient eurekaClient, String vipAddress) {
        return eurekaClient.forVips(vipAddress)
                           .map(notification -> {
                               System.out.println(notification);
                               return notification;
                           })
                .filter(notification -> notification.getKind() == ChangeNotification.Kind.Add) /* Filter all notifications which are not add */
                .map(ChangeNotification::getData) /*Retrieve only the data*/
                .toBlocking()
                .first();
    }

    public static EurekaWriteServer startEurekaServer(int eurekaReadServerPort, int eurekaWriteServerPort)
            throws Exception {
        WriteServerConfig.WriteServerConfigBuilder builder = new WriteServerConfig.WriteServerConfigBuilder();
        builder.withReadServerPort(eurekaReadServerPort)
               .withWriteServerPort(eurekaWriteServerPort)
               .withReplicationPort(8888)
               .withWriteClusterAddresses(new String[] { "127.0.01"})
               .withCodec(EurekaTransports.Codec.Avro)
               .withDataCenterType(LocalDataCenterInfo.DataCenterType.Basic);
        EurekaWriteServer eurekaWriteServer = new EurekaWriteServer(builder.build());

        /* start the server */
        eurekaWriteServer.start();

        System.out.println("Started eureka server....");

        return eurekaWriteServer;
    }

    public static void registerWithEureka(int serverPort, EurekaClient client, String vipAddress) {
        final HashSet<ServicePort> ports = new HashSet<>(Arrays.asList(new ServicePort(serverPort, false)));

        InstanceInfo instance = new InstanceInfo.Builder()
                .withId(String.valueOf(serverPort))
                .withApp("mock_server")
                .withStatus(InstanceInfo.Status.UP)
                .withVipAddress(vipAddress)
                .withPorts(ports)
                .withDataCenterInfo(BasicDataCenterInfo.fromSystemData())
                .build();

        client.register(instance).toBlocking().lastOrDefault(null); // Wait till the registration is successful.
    }

    public static EurekaClient createEurekaClient(int eurekaReadServerPort, int eurekaWriteServerPort) {
        ServerResolver.Server discoveryServer = new ServerResolver.Server("127.0.0.1", eurekaReadServerPort);
        ServerResolver.Server registrationServer = new ServerResolver.Server("127.0.0.1", eurekaWriteServerPort);
        return Eureka.newClientBuilder(ServerResolvers.from(discoveryServer),
                                       ServerResolvers.from(registrationServer))
                     .build();
    }

}
