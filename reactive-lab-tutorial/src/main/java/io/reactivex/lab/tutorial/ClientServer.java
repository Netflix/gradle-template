package io.reactivex.lab.tutorial;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

import java.nio.charset.Charset;
import java.util.Map;

/**
 * This example starts a simple HTTP server and client to demonstrate how to use RxNetty HTTP protocol.
 */
public class ClientServer {

    public static void main(String[] args) {

        /**
         * Start our HTTP server.
         */
        HttpServer<ByteBuf, ByteBuf> server = startServer(8088);

        /**
         * Submit the request.
         */
        createRequest("localhost", server.getServerPort())
                /* Block till you get the response. In a real world application, one should not be blocked but chained
                 * into a response to the caller. */
                .toBlocking()
                /**
                 * Print each content of the response.
                 */
                .forEach(System.out::println);
    }

    public static HttpServer<ByteBuf, ByteBuf> startServer(int port) {

        /**
         * Creates an HTTP server which returns "Hello World!" responses.
         */
        return RxNetty.createHttpServer(port,
                                        /*
                                         * HTTP Request handler for RxNetty where you control what you write as the
                                         * response for each and every request the server receives.
                                         */
                                        (request, response) -> {
                                            /**
                                             * In a real server, you would be writing different responses based on the
                                             * URI of the request.
                                             * This example just returns a "Hello World!!" string unconditionally.
                                             */
                                            return response.writeStringAndFlush("Hello World!!");
                                        })
                      .start();
    }

    public static Observable<String> createRequest(String host, int port) {

        /**
         * Creates an HTTP client bound to the provided host and port.
         */
        return RxNetty.createHttpClient(host, port)
                /* Submit an HTTP GET request with uri "/hello" */
                .submit(HttpClientRequest.createGet("/hello"))
                /* Print the HTTP initial line and headers. Return the content.*/
                .flatMap(response -> {
                    /**
                     * Printing the HTTP initial line.
                     */
                    System.out.println(response.getHttpVersion().text() + ' ' + response.getStatus().code()
                                       + ' ' + response.getStatus().reasonPhrase());
                    /**
                     * Printing HTTP headers.
                     */
                    for (Map.Entry<String, String> header : response.getHeaders().entries()) {
                        System.out.println(header.getKey() + ": " + header.getValue());
                    }

                    // Line break after the headers.
                    System.out.println();

                    return response.getContent();
                })
                /* Convert the ByteBuf for each content chunk into a string. */
                .map(byteBuf -> byteBuf.toString(Charset.defaultCharset()));

    }
}
