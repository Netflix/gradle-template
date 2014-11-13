package io.reactivex.lab.services;

import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.registry.InstanceInfo;
import com.netflix.eureka2.registry.ServicePort;
import com.netflix.eureka2.registry.datacenter.BasicDataCenterInfo;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.sse.ServerSentEvent;
import rx.Observable;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public abstract class MiddleTierService {

    private EurekaClient client;
    private HttpServer<ByteBuf, ServerSentEvent> server;
    protected final String eurekaVipAddress;

    protected MiddleTierService(String eurekaVipAddress, EurekaClient client) {
        this.eurekaVipAddress = eurekaVipAddress;
        this.client = client;
    }

    public HttpServer<ByteBuf, ServerSentEvent> createServer(int port) {
        System.out.println("Start " + getClass().getSimpleName() + " on port: " + port);
        return RxNetty.createHttpServer(port, (request, response) -> {
            // System.out.println("Server => Request: " + request.getPath());
            try {
                return handleRequest(request, response);
            } catch (Throwable e) {
                e.printStackTrace();
                System.err.println("Server => Error [" + request.getPath() + "] => " + e);
                response.setStatus(HttpResponseStatus.BAD_REQUEST);
                return response.writeStringAndFlush("data: Error 500: Bad Request\n" + e.getMessage() + "\n");
            }
        }, PipelineConfigurators.<ByteBuf> serveSseConfigurator());
    }

    public void start(int port) {
        server = createServer(port);
        server.start();
        client.register(createInstanceInfo(port)).toBlocking().lastOrDefault(null);
    }

    public void startAndWait(int port) {
        start(port);
        try {
            server.waitTillShutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        client.unregister(createInstanceInfo(server.getServerPort())).doOnError(Throwable::printStackTrace).subscribe();
        if (null != server) {
            try {
                server.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected abstract Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<ServerSentEvent> response);

    protected InstanceInfo createInstanceInfo(int port) {
        final HashSet<ServicePort> ports = new HashSet<>(Arrays.asList(new ServicePort(port, false)));

        String hostName = "unknown";
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return new InstanceInfo.Builder()
                .withId(hostName + "-" + port)
                .withApp("mock_service")
                .withStatus(InstanceInfo.Status.UP)
                .withVipAddress(eurekaVipAddress)
                .withPorts(ports)
                .withDataCenterInfo(BasicDataCenterInfo.fromSystemData())
                .build();
    }

    protected static Observable<Void> writeError(HttpServerRequest<?> request, HttpServerResponse<?> response, String message) {
        System.err.println("Server => Error [" + request.getPath() + "] => " + message);
        response.setStatus(HttpResponseStatus.BAD_REQUEST);
        return response.writeStringAndFlush("Error 500: " + message + "\n");
    }

    protected static int getParameter(HttpServerRequest<?> request, String key, int defaultValue) {
        List<String> v = request.getQueryParameters().get(key);
        if (v == null || v.size() != 1) {
            return defaultValue;
        } else {
            return Integer.parseInt(String.valueOf(v.get(0)));
        }
    }
}
