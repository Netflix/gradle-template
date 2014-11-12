package io.reactivex.lab.services.impls;

import static org.junit.Assert.assertTrue;
import io.reactivex.lab.services.impls.MockResponse;

import org.junit.Test;

import rx.Observable;

public class MockResponseTest {

    @Test
    public void testJson() throws Exception {
        Observable<String> jsonObservable = MockResponse.generateJson(736L, 1, 1000, 5);
        String json = jsonObservable.toBlocking().single();
        System.out.println(json);
        assertTrue(json.startsWith("{\"responseKey\":" + MockResponse.getResponseKey(736L) + ",\"delay\":1,\"itemSize\":1000,\"numItems\":5,\"items\""));
        System.out.println("json: " + json);
    }

}
