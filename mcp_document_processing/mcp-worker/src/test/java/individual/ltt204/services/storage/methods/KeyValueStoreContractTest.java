package individual.ltt204.services.storage.methods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisClient;

class KeyValueStoreContractTest {
    private StoreFixture currentFixture;

    static Stream<Arguments> stores() {
        RedisClient redisClient = RedisClient.create(
                System.getProperty("redis.url", "redis://:ltt204@localhost:6379"));

        try {
            Assumptions.assumeTrue("PONG".equals(redisClient.ping()),
                    "Redis is not available at the configured redis.url");
        } catch (RuntimeException exception) {
            redisClient.close();
            Assumptions.abort("Redis is not available at the configured redis.url");
        }

        String keyPrefix = "test:contract:" + UUID.randomUUID() + ":";
        return Stream.of(
                Arguments.of(new StoreFixture(
                        "ConcurrentHashMap",
                        new ConcurrentHashMapKeyValueStore<String, String>(),
                        null,
                        null)),
                Arguments.of(new StoreFixture(
                        "Redis",
                        new RedisKeyValueStore<String, String>(redisClient, keyPrefix, String.class, 3, 3600),
                        redisClient,
                        keyPrefix)));
    }

    @AfterEach
    void tearDown() {
        if (currentFixture != null) {
            currentFixture.close();
        }
    }

    @ParameterizedTest(name = "{0} absent-key contract")
    @MethodSource("stores")
    void absentKeyComputeAndComputeIfPresentReturnNull(StoreFixture fixture) {
        currentFixture = fixture;
        boolean[] predicateCalled = { false };

        assertNull(fixture.store.get("missing"));
        assertFalse(fixture.store.computeIfPresent("missing", "value", existing -> {
            predicateCalled[0] = true;
            return false;
        }));
    }

    @ParameterizedTest(name = "{0} putIfAbsent race")
    @MethodSource("stores")
    void putIfAbsentRaceHasExactlyOneWinner(StoreFixture fixture) throws Exception {
        currentFixture = fixture;
        int threadCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> results = IntStream.range(0, threadCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        await(start);
                        return fixture.store.putIfAbsent("race", "value-" + index);
                    }))
                    .toList();

            assertTrue(ready.await(1, TimeUnit.SECONDS));
            assertEquals(1, start.getCount(), "Start latch should be at 1 before starting the race");
            start.countDown();

            long loserResults = 0;
            for (Future<Boolean> result : results) {
                if (result.get(2, TimeUnit.SECONDS) == Boolean.FALSE) {
                    loserResults++;
                }
            }

            long winnerCount = results.stream().filter(future -> {
                try {
                    return future.get(2, TimeUnit.SECONDS) == Boolean.TRUE;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).count();

            assertEquals(7, loserResults);
            assertEquals(1, winnerCount);
            assertTrue(fixture.store.get("race").startsWith("value-"));
        } finally {
            executor.shutdownNow();
        }
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(1, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating contract test", exception);
        }
    }

    private record StoreFixture(
            String name,
            KeyValueStore<String, String> store,
            RedisClient redisClient,
            String keyPrefix) {

        @Override
        public String toString() {
            return name;
        }

        void close() {
            store.remove("missing");
            store.remove("race");
            store.remove("cas");
            if (redisClient != null) {
                redisClient.close();
            }
        }
    }
}