package io.reactivex.lab.gateway.loadbalancer;

import com.netflix.eureka2.interests.Interests;
import com.netflix.eureka2.registry.NetworkAddress.ProtocolType;
import com.netflix.eureka2.registry.ServicePort;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.Host;
import netflix.ocelli.LoadBalancer;
import netflix.ocelli.LoadBalancers;
import netflix.ocelli.MembershipEvent;
import netflix.ocelli.algorithm.LinearWeightingStrategy;
import netflix.ocelli.eureka.EurekaMembershipSource;
import netflix.ocelli.rxnetty.HttpClientHolder;
import netflix.ocelli.rxnetty.HttpClientPool;
import netflix.ocelli.rxnetty.RxNettyFailureDetector;
import netflix.ocelli.rxnetty.RxNettyPendingRequests;
import rx.Observable;

import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A utility to create {@link LoadBalancer} instances for different mid-tier services.
 *
 * @author Nitesh Kant
 */
public class LoadBalancerFactory {

    private final EurekaMembershipSource membershipSource;
    private final HttpClientPool<ByteBuf, ServerSentEvent> clientPool;

    public LoadBalancerFactory(EurekaMembershipSource membershipSource,
                               HttpClientPool<ByteBuf, ServerSentEvent> clientPool) {
        this.membershipSource = membershipSource;
        this.clientPool = clientPool;
    }

    public LoadBalancer<HttpClientHolder<ByteBuf, ServerSentEvent>> forVip(String targetVip) {
        Observable<MembershipEvent<Host>> eurekaHostSource = membershipSource.forInterest(Interests.forVips(targetVip), instanceInfo -> {
            String ipAddress = instanceInfo.getDataCenterInfo()
                    .getAddresses().stream()
                    .filter(na -> na.getProtocolType() == ProtocolType.IPv4)
                    .collect(Collectors.toList()).get(0).getIpAddress();
            HashSet<ServicePort> servicePorts = instanceInfo.getPorts();
            ServicePort portToUse = servicePorts.iterator().next();
            return new Host(ipAddress, portToUse.getPort());
        });

        final Map<Host, HttpClientHolder<ByteBuf, ServerSentEvent>> hostVsHolders = new ConcurrentHashMap<>();

        String lbName = targetVip + "-lb";
        return LoadBalancers.newBuilder(eurekaHostSource.map(
                hostEvent -> {
                    HttpClient<ByteBuf, ServerSentEvent> client = clientPool.getClientForHost(hostEvent.getClient());
                    HttpClientHolder<ByteBuf, ServerSentEvent> holder;
                    if (hostEvent.getType() == MembershipEvent.EventType.REMOVE) {
                        holder = hostVsHolders.remove(hostEvent.getClient());
                    } else {
                        holder = new HttpClientHolder<>(client);
                        hostVsHolders.put(hostEvent.getClient(), holder);
                    }
                    return new MembershipEvent<>(hostEvent.getType(), holder);
                })).withWeightingStrategy(new LinearWeightingStrategy<>(new RxNettyPendingRequests<>()))
                   .withName(lbName)
                .withFailureDetector(new RxNettyFailureDetector<>()).build();
    }
}
