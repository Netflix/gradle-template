package io.reactivex.lab.tutorial;

import com.netflix.eureka2.client.EurekaClient;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixObservableCommand;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import netflix.ocelli.Host;
import netflix.ocelli.MembershipEvent;
import rx.Observable;

import static io.reactivex.lab.tutorial.ClientServerWithDiscovery.createEurekaClient;
import static io.reactivex.lab.tutorial.ClientServerWithLoadBalancer.createEurekaHostStream;

public class ClientServerWithResiliencePatterns {

    public static void main(String[] args) throws Exception {

        ClientServerWithDiscovery.startEurekaServer();

        EurekaClient eurekaClient = createEurekaClient();

        HttpServer<ByteBuf, ServerSentEvent> server = ClientServer.startServer(8089);

        /*HttpServer<ByteBuf, ServerSentEvent> server = ClientServer.startServer(8089, 1, TimeUnit.MILLISECONDS);*/

        String vipAddress = "mock_server-" + server.getServerPort();

        ClientServerWithDiscovery.registerWithEureka(server.getServerPort(), eurekaClient, vipAddress);

        Observable<MembershipEvent<Host>> eurekaHostSource = createEurekaHostStream(eurekaClient, vipAddress);

        MyCommand myCommand = new MyCommand(eurekaHostSource);

        myCommand.toObservable().toBlocking()
                 .forEach((sse) -> System.out.println(sse.contentAsString()));
    }

    public static class MyCommand extends HystrixObservableCommand<ServerSentEvent> {

        private final Observable<MembershipEvent<Host>> eurekaHostSource;

        public MyCommand(Observable<MembershipEvent<Host>> eurekaHostSource) {
            super(HystrixCommandGroupKey.Factory.asKey("MyCommand"));
            this.eurekaHostSource = eurekaHostSource;
        }

        @Override
        protected Observable<ServerSentEvent> run() {
            return ClientServerWithLoadBalancer.createRequestFromLB(eurekaHostSource);
        }

        @Override
        protected Observable<ServerSentEvent> getFallback() {
            return Observable.just(new ServerSentEvent(Unpooled.buffer().writeBytes("Fallback data.".getBytes())));
        }
    }
}
