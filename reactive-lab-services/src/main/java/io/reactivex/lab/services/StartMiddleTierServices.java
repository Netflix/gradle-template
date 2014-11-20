package io.reactivex.lab.services;

import io.reactivex.lab.services.impls.BookmarksService;
import io.reactivex.lab.services.impls.GeoService;
import io.reactivex.lab.services.impls.MockService;
import io.reactivex.lab.services.impls.PersonalizedCatalogService;
import io.reactivex.lab.services.impls.RatingsService;
import io.reactivex.lab.services.impls.SocialService;
import io.reactivex.lab.services.impls.UserService;
import io.reactivex.lab.services.impls.VideoMetadataService;
import rx.Observable;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.client.resolver.ServerResolvers;

public class StartMiddleTierServices {

    public static void main(String... args) {
        /* Eureka Server Config */
        System.setProperty("reactivelab.eureka.server.host", StartEurekaServer.EUREKA_SERVER_HOST);
        System.setProperty("reactivelab.eureka.server.read.port", String.valueOf(StartEurekaServer.EUREKA_SERVER_READ_PORT));
        System.setProperty("reactivelab.eureka.server.write.port", String.valueOf(StartEurekaServer.EUREKA_SERVER_WRITE_PORT));
        System.setProperty("eureka2.registration.heartbeat.intervalMillis", "3000"); // set lower for demo/playground purposes

        /* Create a EurekaClient to be used by the services for registering for discovery */
        ServerResolver.Server discoveryServer = new ServerResolver.Server(StartEurekaServer.EUREKA_SERVER_HOST, StartEurekaServer.EUREKA_SERVER_READ_PORT);
        ServerResolver.Server registrationServer = new ServerResolver.Server(StartEurekaServer.EUREKA_SERVER_HOST, StartEurekaServer.EUREKA_SERVER_WRITE_PORT);
        EurekaClient eurekaClient = Eureka.newClientBuilder(ServerResolvers.from(discoveryServer), ServerResolvers.from(registrationServer)).build();

        /* what port we want to begin at for launching the services */
        int startingPort = 9190;
        if (args.length > 0) {
            startingPort = Integer.parseInt(args[0]);
        }

        System.out.println("Starting services ...");
        new MockService(eurekaClient).start(startingPort);
        new BookmarksService(eurekaClient).start(++startingPort);
        new GeoService(eurekaClient).start(++startingPort);
        new PersonalizedCatalogService(eurekaClient).start(++startingPort);
        new RatingsService(eurekaClient).start(++startingPort);
        new SocialService(eurekaClient).start(++startingPort);
        new UserService(eurekaClient).start(++startingPort);
        new VideoMetadataService(eurekaClient).start(++startingPort);

        // block forever
        Observable.never().toBlocking().single();
    }

}
