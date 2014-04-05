package io.reactivex.lab.middle.nf.services;

import io.reactivex.lab.middle.nf.MiddleTierService;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import rx.Observable;

public class SocialService extends MiddleTierService {

    @Override
    protected Observable<Void> handleRequest(HttpServerRequest<?> request, HttpServerResponse<?> response) {
        // TODO Auto-generated method stub
        return null;
    }
}
