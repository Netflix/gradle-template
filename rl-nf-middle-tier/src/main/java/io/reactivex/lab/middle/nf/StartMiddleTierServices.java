package io.reactivex.lab.middle.nf;

import io.reactivex.lab.middle.nf.services.BookmarksService;
import io.reactivex.lab.middle.nf.services.GeoService;
import io.reactivex.lab.middle.nf.services.PersonalizedCatalogService;
import io.reactivex.lab.middle.nf.services.RatingsService;
import io.reactivex.lab.middle.nf.services.SocialService;
import io.reactivex.lab.middle.nf.services.UserService;
import io.reactivex.lab.middle.nf.services.VideoMetadataService;

public class StartMiddleTierServices {

    public static void main(String... args) {
        System.out.println("Starting services ...");
        new BookmarksService().createServer(9190).start();
        new GeoService().createServer(9191).start();
        new PersonalizedCatalogService().createServer(9192).start();
        new RatingsService().createServer(9193).start();
        new SocialService().createServer(9194).start();
        new UserService().createServer(9195).start();
        new VideoMetadataService().createServer(9196).startAndWait();
    }

}
