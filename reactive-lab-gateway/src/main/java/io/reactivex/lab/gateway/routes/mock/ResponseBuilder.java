package io.reactivex.lab.gateway.routes.mock;

import io.reactivex.netty.protocol.http.server.HttpServerResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import rx.functions.Action0;

/**
 * Build the edge response from the middle-tier responses.
 */
public class ResponseBuilder {

    private final static JsonFactory jsonFactory = new JsonFactory();

    public static void writeTestResponse(Writer writer, BackendResponse responseA, BackendResponse responseB,
            BackendResponse responseC, BackendResponse responseD, BackendResponse responseE) {
        writeTestResponse(writer, responseA, responseB, responseC, responseD, responseE, null);
    }

    public static void writeTestResponse(Writer writer, BackendResponse responseA, BackendResponse responseB,
            BackendResponse responseC, BackendResponse responseD, BackendResponse responseE, Action0 onComplete) {
        try {
            JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
            generateResponse(responseA, responseB, responseC, responseD, responseE, jsonGenerator);
            if (onComplete != null) {
                onComplete.call();
            }
            jsonGenerator.close();
        } catch (Exception e) {
            throw new RuntimeException("failed to generated response", e);
        }

    }

    public static ByteArrayOutputStream buildTestResponse(BackendResponse responseA, BackendResponse responseB,
            BackendResponse responseC, BackendResponse responseD, BackendResponse responseE) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(bos);
            generateResponse(responseA, responseB, responseC, responseD, responseE, jsonGenerator);
            jsonGenerator.close();
            return bos;
        } catch (Exception e) {
            throw new RuntimeException("failed to generated response", e);
        }
    }

    private static void generateResponse(BackendResponse responseA, BackendResponse responseB, BackendResponse responseC, BackendResponse responseD, BackendResponse responseE, JsonGenerator jsonGenerator) throws IOException, JsonGenerationException {
        jsonGenerator.writeStartObject();
        // multiplication of C, D, E responseKey
        jsonGenerator.writeNumberField("responseKey", responseC.getResponseKey() + responseD.getResponseKey() +
                responseE.getResponseKey());

        // delay values of each response
        jsonGenerator.writeArrayFieldStart("delay");
        writeTuple(jsonGenerator, "a", responseA.getDelay());
        writeTuple(jsonGenerator, "b", responseB.getDelay());
        writeTuple(jsonGenerator, "c", responseC.getDelay());
        writeTuple(jsonGenerator, "d", responseD.getDelay());
        writeTuple(jsonGenerator, "e", responseE.getDelay());
        jsonGenerator.writeEndArray();

        // itemSize values of each response
        jsonGenerator.writeArrayFieldStart("itemSize");
        writeTuple(jsonGenerator, "a", responseA.getItemSize());
        writeTuple(jsonGenerator, "b", responseB.getItemSize());
        writeTuple(jsonGenerator, "c", responseC.getItemSize());
        writeTuple(jsonGenerator, "d", responseD.getItemSize());
        writeTuple(jsonGenerator, "e", responseE.getItemSize());
        jsonGenerator.writeEndArray();

        // numItems values of each response
        jsonGenerator.writeArrayFieldStart("numItems");
        writeTuple(jsonGenerator, "a", responseA.getNumItems());
        writeTuple(jsonGenerator, "b", responseB.getNumItems());
        writeTuple(jsonGenerator, "c", responseC.getNumItems());
        writeTuple(jsonGenerator, "d", responseD.getNumItems());
        writeTuple(jsonGenerator, "e", responseE.getNumItems());
        jsonGenerator.writeEndArray();

        // all items from responses
        jsonGenerator.writeArrayFieldStart("items");
        addItemsFromResponse(jsonGenerator, responseA);
        addItemsFromResponse(jsonGenerator, responseB);
        addItemsFromResponse(jsonGenerator, responseC);
        addItemsFromResponse(jsonGenerator, responseD);
        addItemsFromResponse(jsonGenerator, responseE);
        jsonGenerator.writeEndArray();

        jsonGenerator.writeEndObject();
    }

    private static void addItemsFromResponse(JsonGenerator jsonGenerator, BackendResponse a) throws IOException {
        for (String s : a.getItems()) {
            jsonGenerator.writeString(s);
        }
    }

    private static void writeTuple(JsonGenerator jsonGenerator, String name, int value) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeNumberField(name, value);
        jsonGenerator.writeEndObject();
    }

    /**
     * Add various headers used for logging and statistics.
     */
    public static void addResponseHeaders(HttpServerResponse<?> response, long startTime) {
        System.out.println("response headers");
        Map<String, String> perfResponseHeaders = getPerfResponseHeaders(startTime);
        for (Map.Entry<String, String> entry : perfResponseHeaders.entrySet()) {
            response.getHeaders().add(entry.getKey(), entry.getValue());
        }
    }

    private static final OperatingSystemMXBean osStats = ManagementFactory.getOperatingSystemMXBean();

    /**
     * Include performance information as headers.
     * 
     * @param startTime
     * @return
     */
    public static Map<String, String> getPerfResponseHeaders(long startTime) {
        Map<String, String> toReturn = new HashMap<String, String>();
        toReturn.put("server_response_time", String.valueOf((System.currentTimeMillis() - startTime)));

        toReturn.put("os_arch", osStats.getArch());
        toReturn.put("os_name", osStats.getName());
        toReturn.put("os_version", osStats.getVersion());
        toReturn.put("jvm_version", System.getProperty("java.runtime.version"));
        // per core load average
        int cores = Runtime.getRuntime().availableProcessors();
        double loadAverage = osStats.getSystemLoadAverage();
        double loadAveragePerCore = loadAverage / cores;
        String l = String.valueOf(loadAveragePerCore);
        if (l.length() > 4) {
            l = l.substring(0, 4);
        }
        toReturn.put("load_avg_per_core", l);
        return toReturn;
    }
}