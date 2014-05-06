## ReactiveLab

Experiments and prototypes with reactive application design using service-oriented architecture concepts.

### [Edge](https://github.com/benjchristensen/ReactiveLab/tree/master/reactive-lab-edge)


Start server using [EdgeServer.java](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-edge/src/main/java/io/reactivex/lab/edge/EdgeServer.java) or Gradle:

```
./gradlew startEdge
```


### [Services](https://github.com/benjchristensen/ReactiveLab/tree/master/reactive-lab-services)

Simulation of middle-tier RPC/RESTful services exposing endpoints over HTTP.

Start several services on different ports using [StartMiddleTierServices.java](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-services/src/main/java/io/reactivex/lab/services/StartMiddleTierServices.java) or Gradle:

```
./gradlew startServices
```

---------

### Server

See how Netty and RxJava are used as an HTTP server in [EdgeServer](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-edge/src/main/java/io/reactivex/lab/edge/EdgeServer.java#L44)

Basics are:

```java
        RxNetty.createHttpServer(8080, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            ... handle requests here ...
        }).startAndWait();
```

### Client

Clients using Netty and RxJava can be seen in the [clients](https://github.com/benjchristensen/ReactiveLab/tree/master/reactive-lab-edge/src/main/java/io/reactivex/lab/edge/clients) package. 

Basic example:

```java
        return RxNetty.createHttpClient("localhost", 9100)
                .submit(HttpClientRequest.createGet("/mock.json?id=" + id));
```

### Hystrix

Here is a batch request using SSE inside a `HystrixObservableCommand` for fault-tolerance: [BookmarksCommand](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-edge/src/main/java/io/reactivex/lab/edge/clients/BookmarksCommand.java).

A `HystrixObservableCollapser` can be put in front of that command to allow automated batching: [BookmarkCommand](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-edge/src/main/java/io/reactivex/lab/edge/clients/BookmarkCommand.java).

