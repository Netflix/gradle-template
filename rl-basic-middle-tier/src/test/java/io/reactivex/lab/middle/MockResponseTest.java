package io.reactivex.lab.middle;

import static org.junit.Assert.assertTrue;
import io.reactivex.lab.middle.MockResponse;

import org.junit.Test;

import rx.Observable;

public class MockResponseTest {

    @Test
    public void testJson() throws Exception {
        Observable<String> jsonObservable = MockResponse.generateJson(736L, 1, 1000, 5);
        String json = jsonObservable.toBlockingObservable().single();
        System.out.println(json);
        assertTrue(json.startsWith("{\"responseKey\":" + MockResponse.getResponseKey(736L) + ",\"delay\":1,\"itemSize\":1000,\"numItems\":5,\"items\""));
        System.out.println("json: " + json);
    }

}
