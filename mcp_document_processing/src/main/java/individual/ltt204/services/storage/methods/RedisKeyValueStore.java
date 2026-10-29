package individual.ltt204.services.storage.methods;

import java.util.Objects;
import java.util.function.Predicate;

import individual.ltt204.config.Config;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.RedisClient;
import tools.jackson.databind.ObjectMapper;

public class RedisKeyValueStore<K, V> implements KeyValueStore<K, V> {
    private static final int MAX_RETRIES = 5;
    private static final int DEFAULT_EXPIRATION_SECONDS = 60;

    private final RedisClient redisClient;
    private final String keyPrefix;
    private final ObjectMapper objectMapper = Config.getObjectMapper();

    public RedisKeyValueStore(RedisClient redisClient, String keyPrefix) {
        this.redisClient = redisClient;
        this.keyPrefix = keyPrefix;
    }

    @Override
    public V compute(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        redisClient.set(field(key), encode(value));
        return value;
    }

    @Override
    public V compute(K key, V value, Predicate<V> condition) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(condition, "condition");

        return computeWithCas(key, value, condition, MAX_RETRIES);
    }

    @Override
    public V computeIfPresent(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        return computeWithCas(key, value, ignored -> false, MAX_RETRIES);
    }

    @Override
    public V get(K key) {
        Objects.requireNonNull(key, "key");
        String encoded = redisClient.get(field(key));
        if (encoded == null) {
            return null;
        }

        return decode(encoded);
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        long inserted = redisClient.setnx(field(key), encode(value));
        return inserted == 1 ? null : get(key);
    }

    @Override
    public void remove(K key) {
        Objects.requireNonNull(key, "key");
        String redisKey = field(key);
        redisClient.getDel(redisKey);
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("size is not implemented for top-level Redis keys");
    }

    private String field(K key) {
        return keyPrefix + String.valueOf(key);
    }

    private V computeWithCas(K key, V value, Predicate<V> condition, int maxRetries) {
        String redisKey = field(key);

        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try (AbstractTransaction transaction = redisClient.transaction(false)) {
                transaction.watch(redisKey);
                String encoded = transaction.get(redisKey).get();
                V existing = encoded == null ? null : decode(encoded);

                if (condition != null && condition.test(existing)) {
                    transaction.unwatch();
                    return existing;
                }

                transaction.multi();
                transaction.set(redisKey, encode(value));
                transaction.expire(redisKey, DEFAULT_EXPIRATION_SECONDS);
                if (transaction.exec() != null) {
                    return value;
                }
            }
        }

        throw new IllegalStateException("Could not update Redis key after " + maxRetries + " attempts");
    }

    private String encode(V value) {
        try {
            return value.getClass().getName() + "\n" + objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to serialize value", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private V decode(String encoded) {
        try {
            int separator = encoded.indexOf('\n');
            if (separator <= 0) {
                throw new IllegalArgumentException("Invalid stored value");
            }

            Class<?> valueType = Class.forName(encoded.substring(0, separator));
            return (V) objectMapper.readValue(encoded.substring(separator + 1), valueType);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to deserialize value", exception);
        }
    }
}
