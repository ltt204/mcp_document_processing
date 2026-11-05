package individual.ltt204.services.storage.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.RedisClient;

class RedisKeyValueStoreTest {
    private RedisClient redisClient;
    private RedisKeyValueStore<String, String> store;
    private String keyPrefix;

    @BeforeEach
    void setUp() {
        redisClient = RedisClient.create(
                System.getProperty("redis.url", "redis://:ltt204@localhost:6379"));
        try {
            Assumptions.assumeTrue("PONG".equals(redisClient.ping()),
                    "Redis is not available at the configured redis.url");
            keyPrefix = "test:redis-store:" + UUID.randomUUID() + ":";
            store = new RedisKeyValueStore<>(redisClient, keyPrefix);
        } catch (RuntimeException exception) {
            redisClient.close();
            redisClient = null;
            Assumptions.abort("Redis is not available at the configured redis.url");
        }
    }

    @AfterEach
    void tearDown() {
        if (redisClient != null) {
            if (store != null) {
                store.remove("job-1");
                store.remove("put-if-absent");
                store.remove("compute");
                store.remove("cas");
            }
            redisClient.close();
        }
    }

    @Test
    void computeConditionUsesExistingValueAndUpdatesAtomically() {
        store.compute("job-1", "initial");

        boolean result = store.computeIfPresent("job-1", "updated", existing -> existing.equals("initial"));

        assertEquals("initial", result);
        assertEquals("initial", store.get("job-1"));

        result = store.computeIfPresent("job-1", "updated", existing -> !existing.equals("initial"));

        assertEquals("updated", result);
        assertEquals("updated", store.get("job-1"));
    }

    @Test
    void concurrentComputeRetriesWhenBothThreadsReadTheSameValue() throws Exception {
        store.compute("job-1", "initial");

        CountDownLatch bothThreadsReadInitialValue = new CountDownLatch(2);
        CountDownLatch releaseBothThreads = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> updateAfterReadingInitialValue(
                    "first", bothThreadsReadInitialValue, releaseBothThreads));
            Future<Boolean> second = executor.submit(() -> updateAfterReadingInitialValue(
                    "second", bothThreadsReadInitialValue, releaseBothThreads));

            assertTrue(bothThreadsReadInitialValue.await(1, TimeUnit.SECONDS),
                    "Both transactions should read the initial value");
            releaseBothThreads.countDown();

            List<Boolean> results = List.of(first.get(2, TimeUnit.SECONDS), second.get(2, TimeUnit.SECONDS));
            String finalValue = store.get("job-1");

            assertTrue(List.of("first", "second").contains(finalValue));
            assertEquals(finalValue, results.get(0));
            assertEquals(finalValue, results.get(1));
        } finally {
            releaseBothThreads.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void writesFromPutIfAbsentComputeAndCasHaveTtl() {
        store.putIfAbsent("put-if-absent", "value");
        assertTrue(redisClient.ttl(keyPrefix + "put-if-absent") > 0);

        store.compute("compute", "value");
        assertTrue(redisClient.ttl(keyPrefix + "compute") > 0);

        store.compute("cas", "initial");
        store.computeIfPresent("cas", "updated", existing -> false);
        assertTrue(redisClient.ttl(keyPrefix + "cas") > 0);
    }

    private boolean updateAfterReadingInitialValue(
            String newValue,
            CountDownLatch bothThreadsReadInitialValue,
            CountDownLatch releaseBothThreads) {
        return store.computeIfPresent("job-1", newValue, existing -> {
            if ("initial".equals(existing)) {
                bothThreadsReadInitialValue.countDown();
                await(releaseBothThreads);
                return false;
            }

            return true;
        });
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(1, TimeUnit.SECONDS), "Both transactions should be released");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating CAS test", exception);
        }
    }
}
