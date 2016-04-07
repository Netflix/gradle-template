##### Tutorial

This tutorial introduces you to the following libraries:
 
 - [RxJava](https://github.com/ReactiveX/RxJava): Reactive Extensions for the JVM.
 - [RxNetty](https://github.com/ReactiveX/RxNetty): Reactive Extensions to Netty. This is the core networking library of Netflix.
 - [Eureka 2.x](https://github.com/Netflix/eureka/tree/2.x): New version of eureka; Netflix's service discovery system. 
 - Ocelli: Ocelli is an independent project focusing on load balancing needs. 
 - [Hystrix](https://github.com/Netflix/hystrix): Hystrix is netflix's latency and fault tolerance library.
 
The tutorial consists of the following examples which introduces the above libraries incrementally:

###### [ClientServer](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-tutorial/src/main/java/io/reactivex/lab/tutorial/ClientServer.java)

This is the starting point of the tutorial and demonstrates how to write a simple "Hello World" client and server using
[RxNetty](https://github.com/ReactiveX/RxNetty)

###### [ClientServerWithDiscovery](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-tutorial/src/main/java/io/reactivex/lab/tutorial/ClientServerWithDiscovery.java)

This example builds on top of the previous example and adds [Eureka 2.x](https://github.com/Netflix/eureka/tree/2.x) to 
the mix.
The created server registers itself with eureka and the client, instead of hard-coding the host and port of the target 
server, queries the same from eureka.

In order to make the example standalone, an embedded eureka server is started in this example.

###### [ClientServerWithLoadBalancer](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-tutorial/src/main/java/io/reactivex/lab/tutorial/ClientServerWithLoadBalancer.java)

This example builds on top of the previous example and adds Ocelli to the mix.
The client uses ocelli's load balancer instead of directly interacting with eureka.

In order to make the example standalone, an embedded eureka server is started in this example.

###### [ClientServerWithResiliencePatterns](https://github.com/benjchristensen/ReactiveLab/blob/master/reactive-lab-tutorial/src/main/java/io/reactivex/lab/tutorial/ClientServerWithResiliencePatterns.java)

This example builds on top of the previous example and adds [Hystrix](https://github.com/Netflix/hystrix) to the mix.
The client wraps the HTTP client call into a hystrix command, adds artificial delay and demonstrates Hystrix timeout and
fallbac.

In order to make the example standalone, an embedded eureka server is started in this example.


##### How to run?

Each of the above examples have a main method and can be run independently of each other.
