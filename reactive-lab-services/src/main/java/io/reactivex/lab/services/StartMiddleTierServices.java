package io.reactivex.lab.services;

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
        System.out.println("Starting services ...");
        new MockService().createServer(9100).start();
        new BookmarksService().createServer(9190).start();
        new GeoService().createServer(9191).start();
        new PersonalizedCatalogService().createServer(9192).start();
        new RatingsService().createServer(9193).start();
        new SocialService().createServer(9194).start();
        new UserService().createServer(9195).start();
        new VideoMetadataService().createServer(9196).startAndWait();
    }

}
