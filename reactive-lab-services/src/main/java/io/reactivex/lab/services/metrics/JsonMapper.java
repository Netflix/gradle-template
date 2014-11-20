package io.reactivex.lab.services.metrics;

import java.io.IOException;
import java.io.StringWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

/**
 * This code is taken from hystrix-metrics-event-stream module's HystrixMetricsPoller class.
 */
public final class JsonMapper {

    private static final JsonFactory jsonFactory = new JsonFactory();

    private JsonMapper() {
    }

    static String toJson(Metrics commandMetrics) throws IOException {
        StringWriter jsonString = new StringWriter();
        JsonGenerator json = jsonFactory.createJsonGenerator(jsonString);

        json.writeStartObject();
        json.writeStringField("type", "HystrixCommand"); // act as this as we are hijacking Hystrix for the dashboard
        json.writeStringField("name", commandMetrics.getName());
        json.writeStringField("group", "");
        json.writeNumberField("currentTime", System.currentTimeMillis());

        json.writeBooleanField("isCircuitBreakerOpen", false);

        long errors = commandMetrics.getRollingNumber().getRollingSum(Metrics.EventType.FAILURE);
        long success = commandMetrics.getRollingNumber().getRollingSum(Metrics.EventType.SUCCESS);
        long requests = success + errors;
        int errorPercentage = (int) ((double) errors / requests * 100);

        json.writeNumberField("errorPercentage", errorPercentage);
        json.writeNumberField("errorCount", errors);
        json.writeNumberField("requestCount", requests);

        // rolling counters
        json.writeNumberField("rollingCountCollapsedRequests", 0);
        json.writeNumberField("rollingCountExceptionsThrown", commandMetrics.getRollingNumber().getRollingSum(Metrics.EventType.EXCEPTION_THROWN));
        json.writeNumberField("rollingCountFailure", errors);
        json.writeNumberField("rollingCountFallbackFailure", 0);
        json.writeNumberField("rollingCountFallbackRejection", 0);
        json.writeNumberField("rollingCountFallbackSuccess", 0);
        json.writeNumberField("rollingCountResponsesFromCache", 0);
        json.writeNumberField("rollingCountSemaphoreRejected", 0);
        json.writeNumberField("rollingCountShortCircuited", 0);
        json.writeNumberField("rollingCountSuccess", success);
        json.writeNumberField("rollingCountThreadPoolRejected", 0);
        json.writeNumberField("rollingCountTimeout", 0);

        json.writeNumberField("currentConcurrentExecutionCount", commandMetrics.getRollingNumber().getRollingMaxValue(Metrics.EventType.CONCURRENCY_MAX_ACTIVE));

        // latency percentiles
        json.writeNumberField("latencyExecute_mean", commandMetrics.getRollingPercentile().getMean());
        json.writeObjectFieldStart("latencyExecute");
        json.writeNumberField("0", commandMetrics.getRollingPercentile().getPercentile(0));
        json.writeNumberField("25", commandMetrics.getRollingPercentile().getPercentile(25));
        json.writeNumberField("50", commandMetrics.getRollingPercentile().getPercentile(50));
        json.writeNumberField("75", commandMetrics.getRollingPercentile().getPercentile(75));
        json.writeNumberField("90", commandMetrics.getRollingPercentile().getPercentile(90));
        json.writeNumberField("95", commandMetrics.getRollingPercentile().getPercentile(95));
        json.writeNumberField("99", commandMetrics.getRollingPercentile().getPercentile(99));
        json.writeNumberField("99.5", commandMetrics.getRollingPercentile().getPercentile(99.5));
        json.writeNumberField("100", commandMetrics.getRollingPercentile().getPercentile(100));
        json.writeEndObject();

        json.writeNumberField("latencyTotal_mean", commandMetrics.getRollingPercentile().getMean());
        json.writeObjectFieldStart("latencyTotal");
        json.writeNumberField("0", commandMetrics.getRollingPercentile().getPercentile(0));
        json.writeNumberField("25", commandMetrics.getRollingPercentile().getPercentile(25));
        json.writeNumberField("50", commandMetrics.getRollingPercentile().getPercentile(50));
        json.writeNumberField("75", commandMetrics.getRollingPercentile().getPercentile(75));
        json.writeNumberField("90", commandMetrics.getRollingPercentile().getPercentile(90));
        json.writeNumberField("95", commandMetrics.getRollingPercentile().getPercentile(95));
        json.writeNumberField("99", commandMetrics.getRollingPercentile().getPercentile(99));
        json.writeNumberField("99.5", commandMetrics.getRollingPercentile().getPercentile(99.5));
        json.writeNumberField("100", commandMetrics.getRollingPercentile().getPercentile(100));
        json.writeEndObject();
        
        json.writeNumberField("propertyValue_circuitBreakerRequestVolumeThreshold", 0);
        json.writeNumberField("propertyValue_circuitBreakerSleepWindowInMilliseconds", 0);
        json.writeNumberField("propertyValue_circuitBreakerErrorThresholdPercentage", 0);
        json.writeBooleanField("propertyValue_circuitBreakerForceOpen", false);
        json.writeBooleanField("propertyValue_circuitBreakerForceClosed", false);
        json.writeBooleanField("propertyValue_circuitBreakerEnabled", false);

        json.writeStringField("propertyValue_executionIsolationStrategy", "");
        json.writeNumberField("propertyValue_executionIsolationThreadTimeoutInMilliseconds", 0);
        json.writeBooleanField("propertyValue_executionIsolationThreadInterruptOnTimeout", false);
        json.writeStringField("propertyValue_executionIsolationThreadPoolKeyOverride", "");
        json.writeNumberField("propertyValue_executionIsolationSemaphoreMaxConcurrentRequests", 0);
        json.writeNumberField("propertyValue_fallbackIsolationSemaphoreMaxConcurrentRequests", 0);

        json.writeNumberField("propertyValue_metricsRollingStatisticalWindowInMilliseconds", 10000);

        json.writeBooleanField("propertyValue_requestCacheEnabled", false);
        json.writeBooleanField("propertyValue_requestLogEnabled", false);

        
        json.writeNumberField("reportingHosts", 1); // this will get summed across all instances in a cluster

        json.writeEndObject();
        json.close();

        return jsonString.getBuffer().toString();
    }

}