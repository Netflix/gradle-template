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

Basic example with request/response:

```java
        return RxNetty.createHttpClient("localhost", 9100)
                .submit(HttpClientRequest.createGet("/mock.json?numItems=" + numItems + "&itemSize=" + itemSize + "&delay=" + delay + "&id=" + id));
```

and then expanding on that to convert the response into a different format:

```java
        return RxNetty.createHttpClient("localhost", 9100)
                .submit(HttpClientRequest.createGet("/mock.json?numItems=" + numItems + "&itemSize=" + itemSize + "&delay=" + delay + "&id=" + id))
                .flatMap((HttpClientResponse<ByteBuf> r) -> {
                    Observable<BackendResponse> bytesToJson = r.getContent().map(b -> {
                        return BackendResponse.fromJson(new ByteBufInputStream(b));
                    });
                    return bytesToJson;
                });
```


Another example but this time with server-sent-events (SSE):

```java
        return RxNettySSE.createHttpClient("localhost", 9190)
                .submit(HttpClientRequest.createGet("/bookmarks?" + UrlGenerator.generate("videoId", videos)))
                .flatMap(r -> {
                    Observable<Bookmark> bytesToJson = r.getContent().map(sse -> {
                        return Bookmark.fromJson(sse.getEventData());
                    });
                    return bytesToJson;
                });
```
