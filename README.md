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

