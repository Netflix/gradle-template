package io.reactivex.lab.tutorial;

import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.interests.Interests;
import com.netflix.eureka2.registry.NetworkAddress;
import com.netflix.eureka2.registry.ServicePort;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.Host;
import netflix.ocelli.LoadBalancers;
import netflix.ocelli.MembershipEvent;
import netflix.ocelli.eureka.EurekaMembershipSource;
import rx.Observable;

import java.util.HashSet;
import java.util.stream.Collectors;

public class ClientServerWithLoadBalancer {

    public static void main(String[] args) throws Exception {

        ClientServerWithDiscovery.startEurekaServer();

        EurekaClient eurekaClient = ClientServerWithDiscovery.createEurekaClient();

        HttpServer<ByteBuf, ServerSentEvent> server = ClientServer.startServer(8089);

        String vipAddress = "mock_server-" + server.getServerPort();

        ClientServerWithDiscovery.registerWithEureka(server.getServerPort(), eurekaClient, vipAddress);

        Observable<MembershipEvent<Host>> eurekaHostSource = createEurekaHostStream(eurekaClient, vipAddress);

        createRequestFromLB(eurekaHostSource)
                   .toBlocking()
                   .forEach((sse) -> System.out.println(sse.contentAsString()));
    }

    public static Observable<ServerSentEvent> createRequestFromLB(Observable<MembershipEvent<Host>> eurekaHostSource) {
        return LoadBalancers.fromHostSource(eurekaHostSource.map(
                hostEvent -> {
                    Host host = hostEvent.getClient();
                    HttpClient<ByteBuf, ServerSentEvent> client =
                            RxNetty.createHttpClient(host.getHostName(), host.getPort(),
                                                     PipelineConfigurators.clientSseConfigurator());
                    return new MembershipEvent<>(hostEvent.getType(), client);
                })).choose()
                   .flatMap(client -> client.submit(HttpClientRequest.createGet("/"))
                                            .flatMap(response -> response.getContent()));
    }

    public static Observable<MembershipEvent<Host>> createEurekaHostStream(EurekaClient eurekaClient,
                                                                            String vipAddress) {
        EurekaMembershipSource membershipSource = new EurekaMembershipSource(eurekaClient);

        eurekaClient.forInterest(Interests.forFullRegistry()).take(1).toBlocking().first();// Warm up eureka data

        return membershipSource.forInterest(Interests.forVips(vipAddress), instanceInfo -> {
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
