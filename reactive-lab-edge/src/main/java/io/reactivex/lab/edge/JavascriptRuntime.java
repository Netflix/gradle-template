package io.reactivex.lab.edge;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.text.sse.ServerSentEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import rx.Observable;

public class JavascriptRuntime {

    private final ScriptEngineManager engineManager = new ScriptEngineManager();
    private final ConcurrentHashMap<String, ScriptEngine> endpoints = new ConcurrentHashMap<>();

    private static final JavascriptRuntime INSTANCE = new JavascriptRuntime();

    public static JavascriptRuntime getInstance() {
        return INSTANCE;
    }

    private JavascriptRuntime() {
        // bind the APIServiceLayer as 'api' so Javascript scripts can access it
        engineManager.put("api", new APIServiceLayer());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Observable<Void> handleRequest(HttpServerRequest<ByteBuf> request, HttpServerResponse<ServerSentEvent> response) {
        try {
            Invocable i = loadEndpoint(request.getPath().substring(1));
            response.setStatus(HttpResponseStatus.OK);
            Object o = i.invokeFunction("execute", request.getQueryParameters());
            /*
             * For this prototype we support blocking and non-blocking Javascript functions
             */
            if (o instanceof Observable) {
                return ((Observable) o).flatMap(d -> {
                    return response.writeStringAndFlush(String.valueOf(d) + "\n");
                });
            } else {
                System.out.println("o: " + o.getClass());
                return response.writeStringAndFlush(String.valueOf(o) + "\n");
            }
        } catch (Throwable e) {
            System.err.println("Error [" + request.getPath() + "] => " + e);
            response.setStatus(HttpResponseStatus.NOT_FOUND);
            return response.writeStringAndFlush("Error 404: Not Found\n");
        }
    }

    private Invocable loadEndpoint(String name) throws ScriptException {
        ScriptEngine se = endpoints.get(name);
        if (se != null) {
            return (Invocable) se;
        }
        try (InputStream is = JavascriptRuntime.class.getResourceAsStream("/jsx/endpoints/" + name)) {
            if (is == null) {
                throw new RuntimeException("Endpoint not found: " + name);
            }
            ScriptEngine engine = engineManager.getEngineByName("nashorn");
            engine.eval(new InputStreamReader(is));
            //            endpoints.put(name, engine); // comment out to reload JS each time

            /*
             * Hardcoding loading of 3rd party library for prototyping
             */
            try (InputStream bootstrap_interval = JavascriptRuntime.class.getResourceAsStream("/jsx/jslib/bootstrap_interval.js")) {
                engine.eval(new InputStreamReader(bootstrap_interval));
            }
            try (InputStream mustacheInputStream = JavascriptRuntime.class.getResourceAsStream("/jsx/jslib/mustache.js")) {
                engine.eval(new InputStreamReader(mustacheInputStream));
            }
            // experimenting with RxJS versus RxJava from Nashorn
            try (InputStream mustacheInputStream = JavascriptRuntime.class.getResourceAsStream("/jsx/jslib/rx.js")) {
                engine.eval(new InputStreamReader(mustacheInputStream));
            }
            return (Invocable) engine;
        } catch (IOException e) {
            throw new RuntimeException("Unable to open resource: " + name);
        }
    }

}
