package io.reactivex.lab.edge.nf;

import io.reactivex.lab.edge.nf.clients.UserCommand;
import io.reactivex.lab.edge.nf.clients.UserCommand.User;

import java.util.Arrays;

import rx.Observable;

public class APIServiceLayer {

    public Observable<String> getData() {
        return Observable.from("one", "two", "three");
    }

    public String hello(String name) {
        return "Hello " + name + "!";
    }

    public Observable<User> getUser(int userId) {
        return new UserCommand(Arrays.asList(String.valueOf(userId))).observe();
    }
}
