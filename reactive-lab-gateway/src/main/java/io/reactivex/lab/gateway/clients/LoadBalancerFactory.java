package io.reactivex.lab.gateway.clients;

import com.netflix.eureka2.interests.Interests;
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
        Observable<MembershipEvent<Host>> eurekaHostSource = membershipSource.forInterest(Interests.forVips(targetVip));

        return LoadBalancers.newBuilder(eurekaHostSource.map(
                        hostEvent -> {
                            HttpClient<ByteBuf, ServerSentEvent> client = clientPool.getClientForHost(hostEvent.getClient());
                            return new MembershipEvent<>(hostEvent.getType(), new HttpClientHolder<>(client));
                        })).withWeightingStrategy(new LinearWeightingStrategy<>(new RxNettyPendingRequests<>()))
                           .withFailureDetector(new RxNettyFailureDetector<>()).build();
    }
}
