package io.reactivex.lab.services.metrics;

import com.netflix.numerus.NumerusProperty;
import com.netflix.numerus.NumerusRollingNumber;
import com.netflix.numerus.NumerusRollingNumberEvent;
import com.netflix.numerus.NumerusRollingPercentile;

public class Metrics {

    private static final NumerusProperty<Integer> latency_timeInMilliseconds = NumerusProperty.Factory.asProperty(60000);
    private static final NumerusProperty<Integer> latency_numberOfBuckets = NumerusProperty.Factory.asProperty(12); // 12 buckets at 5000ms each
    private static final NumerusProperty<Integer> latency_bucketDataLength = NumerusProperty.Factory.asProperty(1000);
    private static final NumerusProperty<Boolean> latency_enabled = NumerusProperty.Factory.asProperty(true);

    private static final NumerusProperty<Integer> count_timeInMilliseconds = NumerusProperty.Factory.asProperty(10000);
    private static final NumerusProperty<Integer> count_numberOfBuckets = NumerusProperty.Factory.asProperty(10); // 11 buckets at 1000ms each

    private final NumerusRollingPercentile p = new NumerusRollingPercentile(latency_timeInMilliseconds, latency_numberOfBuckets, latency_bucketDataLength, latency_enabled);
    private final NumerusRollingNumber n = new NumerusRollingNumber(EventType.BOOTSTRAP, count_timeInMilliseconds, count_numberOfBuckets);

    private final String name;

    public Metrics(String name) {
        this.name = name;
    }

    public NumerusRollingPercentile getRollingPercentile() {
        return p;
    }

    public NumerusRollingNumber getRollingNumber() {
        return n;
    }

    public String getName() {
        return name;
    }

    public static enum EventType implements NumerusRollingNumberEvent {
        BOOTSTRAP(1), 
        SUCCESS(1), FAILURE(1), EXCEPTION_THROWN(1),
        CONCURRENCY_MAX_ACTIVE(2);

        private final int type;

        EventType(int type) {
            this.type = type;
        }

        public boolean isCounter() {
            return type == 1;
        }

        public boolean isMaxUpdater() {
            return type == 2;
        }

        @Override
        public EventType[] getValues() {
            return values();
        }

    }
}
