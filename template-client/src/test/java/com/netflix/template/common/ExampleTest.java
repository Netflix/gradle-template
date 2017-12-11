package com.netflix.template.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class ExampleTest {
    @Test
    public void canaryTest(TestInfo testinfo) {
        assertEquals(1/*expected*/, 1/*result from your code*/, "canary test 1 should equal 1");
    }
}
