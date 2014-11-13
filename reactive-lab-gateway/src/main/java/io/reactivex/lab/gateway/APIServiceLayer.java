package io.reactivex.lab.gateway;

import io.reactivex.lab.gateway.clients.UserCommand;
import io.reactivex.lab.gateway.clients.UserCommand.User;

import java.util.Arrays;

import rx.Observable;

public class APIServiceLayer {

    public Observable<String> getData() {
        return Observable.just("one", "two", "three");
    }

    public String hello(String name) {
        return "Hello " + name + "!";
    }

    public Observable<User> getUser(int userId) {
        return new UserCommand(Arrays.asList(String.valueOf(userId))).observe();
    }
}
