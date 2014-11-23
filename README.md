## ReactiveLab

Experiments and prototypes with reactive application design using service-oriented architecture concepts.

### [Discovery](https://github.com/benjchristensen/ReactiveLab/tree/master/reactive-lab-services)

Start discovery server using [StartEurekaServer.java](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-services/src/main/java/io/reactivex/lab/services/StartEurekaServer.java)

```
./gradlew startDiscovery
```

### [Gateway](https://github.com/benjchristensen/ReactiveLab/tree/master/reactive-lab-gateway)

Start gateway server using [StartGatewayServer.java](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-gateway/src/main/java/io/reactivex/lab/gateway/StartGatewayServer.java) or Gradle:

```
./gradlew startGateway
```

### [Services](https://github.com/benjchristensen/ReactiveLab/tree/master/reactive-lab-services)

Simulation of middle-tier RPC/RESTful services exposing endpoints over HTTP.

Start several services on different ports using [StartMiddleTierServices.java](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-services/src/main/java/io/reactivex/lab/services/StartMiddleTierServices.java) or Gradle:

```
./gradlew startServices
```

---------

### Server

See how Netty and RxJava are used as an HTTP server in [GatewayServer](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-gateway/src/main/java/io/reactivex/lab/gateway/StartGatewayServer.java#L40)

Basics are:

```java
        RxNetty.createHttpServer(8080, (request, response) -> {
            System.out.println("Server => Request: " + request.getPath());
            ... handle requests here ...
        }).startAndWait();
```

### Client

Clients using Netty and RxJava can be seen in the [clients](https://github.com/benjchristensen/ReactiveLab/tree/master/reactive-lab-gateway/src/main/java/io/reactivex/lab/gateway/clients) package. 

Basic example:

```java
        return RxNetty.createHttpClient("localhost", 9100)
                .submit(HttpClientRequest.createGet("/mock.json?id=" + id));
```

### Hystrix

Here is a batch request using SSE inside a `HystrixObservableCommand` for fault-tolerance: [BookmarksCommand](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-gateway/src/main/java/io/reactivex/lab/gateway/clients/BookmarksCommand.java).

A `HystrixObservableCollapser` can be put in front of that command to allow automated batching: [BookmarkCommand](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-gateway/src/main/java/io/reactivex/lab/gateway/clients/BookmarkCommand.java).


### Composition

Nested, parallel execution of network requests can be composed using RxJava and Hystrix as demonstrated in [RouteForDeviceHome](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-gateway/src/main/java/io/reactivex/lab/gateway/routes/RouteForDeviceHome.java) which is the example running at the `/device/home` endpoint.

Here is a portion of the code to show the composition:

```java
        return new UserCommand(userId).observe().flatMap(user -> {
            Observable<Map<String, Object>> catalog = new PersonalizedCatalogCommand(user).observe()
                    .flatMap(catalogList -> {
                        return catalogList.videos().<Map<String, Object>> flatMap(video -> {
                            Observable<Bookmark> bookmark = new BookmarkCommand(video).observe();
                            Observable<Rating> rating = new RatingsCommand(video).observe();
                            Observable<VideoMetadata> metadata = new VideoMetadataCommand(video).observe();
                            return Observable.zip(bookmark, rating, metadata, (b, r, m) -> {
                                return combineVideoData(video, b, r, m);
                            });
                        });
                    });

            Observable<Map<String, Object>> social = new SocialCommand(user).observe().map(s -> {
                return s.getDataAsMap();
            });

            return Observable.merge(catalog, social);
        }).flatMap(data -> {
            return response.writeAndFlush(new ServerSentEvent("", "data", SimpleJson.mapToJson(data)), EdgeServer.SSE_TRANSFORMER);
        });
```

This results in 7 network calls being made, and multiple bookmark requests are automatically collapsed into 1 network call. Here is the `HystrixRequestLog` that results from the code above being executed:


```
Server => Hystrix Log [/device/home] => UserCommand[SUCCESS][191ms], PersonalizedCatalogCommand[SUCCESS][50ms], SocialCommand[SUCCESS][53ms], RatingsCommand[SUCCESS][65ms]x6, VideoMetadataCommand[SUCCESS][73ms]x6, BookmarksCommand[SUCCESS, COLLAPSED][25ms], BookmarksCommand[SUCCESS, COLLAPSED][24ms]
```
