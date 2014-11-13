package io.reactivex.lab.gateway.mock;

import static junit.framework.Assert.assertEquals;
import io.reactivex.lab.gateway.routes.mock.BackendResponse;

import org.junit.Test;

public class BackendResponseTest {

    @Test
    public void testJsonParse() throws Exception {
        BackendResponse r = BackendResponse.fromJson("{ \"responseKey\": 9999, \"delay\": 50, \"itemSize\": 128, \"numItems\": 2, \"items\": [ \"Lorem\", \"Ipsum\" ]}");
        assertEquals(9999, r.getResponseKey());
        assertEquals(50, r.getDelay());
        assertEquals(128, r.getItemSize());
        assertEquals(2, r.getNumItems());
        String[] items = r.getItems();
        assertEquals(2, items.length);
        assertEquals("Lorem", items[0]);
        assertEquals("Ipsum", items[1]);
    }
}
