package io.reactivex.lab.gateway.routes.mock;

import java.io.IOException;
import java.io.InputStream;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.schedulers.Schedulers;

public class BackendResponse {

    private final static JsonFactory jsonFactory = new JsonFactory();

    private final long responseKey;
    private final int delay;
    private final int numItems;
    private final int itemSize;
    private final String[] items;

    public BackendResponse(long responseKey, int delay, int numItems, int itemSize, String[] items) {
        this.responseKey = responseKey;
        this.delay = delay;
        this.numItems = numItems;
        this.itemSize = itemSize;
        this.items = items;
    }

    public static BackendResponse fromJson(InputStream inputStream) {
        try {
            JsonParser parser = jsonFactory.createJsonParser(inputStream);
            return parseBackendResponse(parser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public static BackendResponse fromJson(String json) {
        try {
            JsonParser parser = jsonFactory.createJsonParser(json);
            return parseBackendResponse(parser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON", e);
        }
    }

    public static Observable<BackendResponse> fromJsonToObservable(InputStream inputStream) {
        return fromJsonToObservable(inputStream, Schedulers.computation());
    }

    public static Observable<BackendResponse> fromJsonToObservable(final InputStream inputStream, Scheduler scheduler) {
        return Observable.create((Subscriber<? super BackendResponse> o) -> {
            try {
                o.onNext(fromJson(inputStream));
                o.onCompleted();
            } catch (Exception e) {
                o.onError(e);
            }
        }).subscribeOn(scheduler);
    }

    private static BackendResponse parseBackendResponse(JsonParser parser) throws IOException {
        try {
            // Sanity check: verify that we got "Json Object":
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                throw new IOException("Expected data to start with an Object");
            }
            long responseKey = 0;
            int delay = 0;
            int numItems = 0;
            int itemSize = 0;
            String[] items = null;
            JsonToken current;

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = parser.getCurrentName();
                // advance
                current = parser.nextToken();
                if (fieldName.equals("responseKey")) {
                    responseKey = parser.getLongValue();
                } else if (fieldName.equals("delay")) {
                    delay = parser.getIntValue();
                } else if (fieldName.equals("itemSize")) {
                    itemSize = parser.getIntValue();
                } else if (fieldName.equals("numItems")) {
                    numItems = parser.getIntValue();
                } else if (fieldName.equals("items")) {
                    // expect numItems to be populated before hitting this
                    if (numItems == 0) {
                        throw new IllegalStateException("Expected numItems > 0");
                    }
                    items = new String[numItems];
                    if (current == JsonToken.START_ARRAY) {
                        int j = 0;
                        // For each of the records in the array
                        while (parser.nextToken() != JsonToken.END_ARRAY) {
                            items[j++] = parser.getText();
                        }
                    } else {
                        //                            System.out.println("Error: items should be an array: skipping.");
                        parser.skipChildren();
                    }

                }
            }
            return new BackendResponse(responseKey, delay, numItems, itemSize, items);
        } finally {
            parser.close();
        }
    }

    public long getResponseKey() {
        return responseKey;
    }

    public int getDelay() {
        return delay;
    }

    public int getNumItems() {
        return numItems;
    }

    public int getItemSize() {
        return itemSize;
    }

    public String[] getItems() {
        return items;
    }

}