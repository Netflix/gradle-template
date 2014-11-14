package io.reactivex.lab.tutorial;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.client.resolver.ServerResolvers;
import com.netflix.eureka2.interests.ChangeNotification;
import com.netflix.eureka2.registry.InstanceInfo;
import com.netflix.eureka2.registry.ServicePort;
import com.netflix.eureka2.registry.datacenter.BasicDataCenterInfo;
import com.netflix.eureka2.registry.datacenter.LocalDataCenterInfo;
import com.netflix.eureka2.server.EurekaWriteServer;
import com.netflix.eureka2.server.WriteServerConfig;
import com.netflix.eureka2.transport.EurekaTransports;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ClientServerWithDiscovery {

    public static final int EUREKA_SERVER_READ_PORT = 7005;
    public static final int EUREKA_SERVER_WRITE_PORT = 7006;

    public static void main(String[] args) throws Exception {
        startEurekaServer(); // start the eureka server.
        EurekaClient eurekaClient = createEurekaClient();

        HttpServer<ByteBuf, ServerSentEvent> server = ClientServer.startServer(8089);

        String vipAddress = "mock_server-" + server.getServerPort();

        registerWithEureka(server.getServerPort(), eurekaClient, vipAddress);

        InstanceInfo serverInfo = getServerInfo(eurekaClient, vipAddress);

        Integer port = serverInfo.getPorts().stream().collect(Collectors.toList()).get(0).getPort();

        ClientServer.createRequest(port)
                    .toBlocking()
                    .forEach(sse -> System.out.println(sse.contentAsString()));
    }

    private static InstanceInfo getServerInfo(EurekaClient eurekaClient, String vipAddress) {
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

    public static void startEurekaServer() throws Exception {
        WriteServerConfig.WriteServerConfigBuilder builder = new WriteServerConfig.WriteServerConfigBuilder();
        builder.withReadServerPort(EUREKA_SERVER_READ_PORT)
               .withWriteServerPort(EUREKA_SERVER_WRITE_PORT)
               .withReplicationPort(8888)
               .withWriteClusterAddresses(new String[] { "127.0.01"})
               .withCodec(EurekaTransports.Codec.Avro)
               .withDataCenterType(LocalDataCenterInfo.DataCenterType.Basic);
        EurekaWriteServer eurekaWriteServer = new EurekaWriteServer(builder.build());

        /* start the server */
        eurekaWriteServer.start();

        System.out.println("Started eureka server....");
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

    public static EurekaClient createEurekaClient() {
        ServerResolver.Server discoveryServer = new ServerResolver.Server("127.0.0.1", EUREKA_SERVER_READ_PORT);
        ServerResolver.Server registrationServer = new ServerResolver.Server("127.0.0.1", EUREKA_SERVER_WRITE_PORT);
        return Eureka.newClientBuilder(ServerResolvers.from(discoveryServer),
                                       ServerResolvers.from(registrationServer))
                     .build();
    }

}
