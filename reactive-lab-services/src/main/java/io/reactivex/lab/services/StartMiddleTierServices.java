package io.reactivex.lab.services;

import com.netflix.eureka2.client.Eureka;
import com.netflix.eureka2.client.EurekaClient;
import com.netflix.eureka2.client.resolver.ServerResolver;
import com.netflix.eureka2.client.resolver.ServerResolvers;
import com.netflix.eureka2.transport.EurekaTransports;
import io.reactivex.lab.services.impls.BookmarksService;
import io.reactivex.lab.services.impls.GeoService;
import io.reactivex.lab.services.impls.MockService;
import io.reactivex.lab.services.impls.PersonalizedCatalogService;
import io.reactivex.lab.services.impls.RatingsService;
import io.reactivex.lab.services.impls.SocialService;
import io.reactivex.lab.services.impls.UserService;
import io.reactivex.lab.services.impls.VideoMetadataService;

public class StartMiddleTierServices {

    public static void main(String... args) {
        int startingPort = 9190;

        if (args.length > 0) {
            startingPort = Integer.parseInt(args[0]);
        }

        ServerResolver.Server discoveryServer = new ServerResolver.Server("127.0.0.1", StartEurekaServer.READ_SERVER_PORT);
        ServerResolver.Server registrationServer = new ServerResolver.Server("127.0.0.1", StartEurekaServer.WRITE_SERVER_PORT);
        EurekaClient client = Eureka.newClientBuilder(ServerResolvers.from(discoveryServer),
                                                      ServerResolvers.from(registrationServer))
                                    .withCodec(EurekaTransports.Codec.Json)
                                    .build();

        System.out.println("Starting services ...");

        MockService mockService = new MockService(client);
        mockService.start(9100);
        BookmarksService bookmarksService = new BookmarksService(client);
        bookmarksService.start(startingPort);
        GeoService geoService = new GeoService(client);
        geoService.start(++startingPort);
        PersonalizedCatalogService personalizedCatalogService = new PersonalizedCatalogService(client);
        personalizedCatalogService.start(++startingPort);
        RatingsService ratingsService = new RatingsService(client);
        ratingsService.start(++startingPort);
        SocialService socialService = new SocialService(client);
        socialService.start(++startingPort);
        UserService userService = new UserService(client);
        userService.start(++startingPort);
        VideoMetadataService videoMetadataService = new VideoMetadataService(client);
        videoMetadataService.startAndWait(++startingPort);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            mockService.stop();
            bookmarksService.stop();
            geoService.stop();
            personalizedCatalogService.stop();
            ratingsService.stop();
            socialService.stop();
            userService.stop();
            videoMetadataService.stop();
        }));
    }

}
