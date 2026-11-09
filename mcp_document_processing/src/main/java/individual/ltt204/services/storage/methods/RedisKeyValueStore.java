package individual.ltt204.services.storage.methods;

import java.util.List;
import java.util.function.Predicate;

import individual.ltt204.config.Config;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.Response;
import redis.clients.jedis.params.SetParams;
import tools.jackson.databind.ObjectMapper;

public class RedisKeyValueStore<K, V> implements KeyValueStore<K, V> {
    private static final int MAX_RETRIES = 5;
    private static final int DEFAULT_EXPIRATION_SECONDS = 60;

    private final RedisClient redisClient;
    private final String keyPrefix;
    private final ObjectMapper objectMapper = Config.getObjectMapper();
    private final Class<V> valueType;

    public RedisKeyValueStore(RedisClient redisClient, String keyPrefix, Class<V> valueType) {
        this.redisClient = redisClient;
        this.keyPrefix = keyPrefix;
        this.valueType = valueType;
    }

    @Override
    public boolean computeIfPresent(K key, V value, Predicate<V> skipComputionPredicate) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try (AbstractTransaction atx = redisClient.transaction(false)) {
                String redisKey = keyPrefix + key.toString();
                atx.watch(redisKey);

                Response<String> currentValue = atx.get(redisKey);

                if (currentValue.get() == null) {
                    return false; // Key is not present, nothing to compute
                }
                V existingValue = objectMapper.readValue(currentValue.get(), valueType);

                if (skipComputionPredicate.test(existingValue)) {
                    atx.unwatch();
                    return false; // Condition not satisfied, nothing to compute
                }

                String newValueJson = objectMapper.writeValueAsString(value);

                atx.multi();
                atx.set(redisKey, newValueJson, SetParams.setParams().ex(DEFAULT_EXPIRATION_SECONDS));
                List<Object> result = atx.exec();

                if (result == null) {
                    // Transaction failed due to a watch condition, retry
                    continue;
                }

                return true;

            } catch (Exception e) {
                // Handle the exception (e.g., log it)
                System.err.println("Error during computeIfPresent: " + e.getMessage());
                // Optionally, you can add a small delay before retrying
                try {
                    Thread.sleep(100); // Sleep for 100 milliseconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    throw new RuntimeException("Thread interrupted during retry", ie);
                }
            }
        }

        throw new RuntimeException("Failed to computeIfPresent after " + MAX_RETRIES + " retries");

    }

    @Override
    public boolean computeIfPresent(K key, V value) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try (AbstractTransaction atx = redisClient.transaction(false)) {
                String redisKey = keyPrefix + key.toString();
                atx.watch(redisKey);

                Response<String> currentValue = atx.get(redisKey);

                if (currentValue.get() != null) {
                    String newValueJson = objectMapper.writeValueAsString(value);

                    atx.multi();
                    atx.set(redisKey, newValueJson, SetParams.setParams().ex(DEFAULT_EXPIRATION_SECONDS));
                    List<Object> result = atx.exec();

                    if (result == null) {
                        // Transaction failed due to a watch condition, retry
                        continue;
                    }

                    return true;
                }

                return false;
            } catch (Exception e) {
                // Handle the exception (e.g., log it)
                System.err.println("Error during computeIfPresent: " + e.getMessage());
                // Optionally, you can add a small delay before retrying
                try {
                    Thread.sleep(100); // Sleep for 100 milliseconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    throw new RuntimeException("Thread interrupted during retry", ie);
                }
            }
        }

        throw new RuntimeException("Failed to computeIfPresent after " + MAX_RETRIES + " retries");
    }

    @Override
    public boolean compute(K key, V value) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try (AbstractTransaction atx = redisClient.transaction(false)) {
                String redisKey = keyPrefix + key.toString();
                atx.watch(redisKey);

                String newValueJson = objectMapper.writeValueAsString(value);

                atx.multi();
                atx.set(redisKey, newValueJson, SetParams.setParams().ex(DEFAULT_EXPIRATION_SECONDS));
                List<Object> result = atx.exec();

                if (result == null) {
                    // Transaction failed due to a watch condition, retry
                    continue;
                }

                return true;
            } catch (Exception e) {
                // Handle the exception (e.g., log it)
                System.err.println("Error during compute: " + e.getMessage());
                // Optionally, you can add a small delay before retrying
                try {
                    Thread.sleep(100); // Sleep for 100 milliseconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    throw new RuntimeException("Thread interrupted during retry", ie);
                }
            }
        }

        throw new RuntimeException("Failed to compute after " + MAX_RETRIES + " retries");
    }

    @Override
    public V get(K key) {
        try {
            String redisKey = keyPrefix + key.toString();
            String valueJson = redisClient.get(redisKey);

            if (valueJson != null) {
                return objectMapper.readValue(valueJson, valueType);
            }
        } catch (Exception e) {
            // Handle the exception (e.g., log it)
            System.err.println("Error during get: " + e.getMessage());
        }

        return null;
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        for (int i = 0; i < MAX_RETRIES; i++) {
            try (AbstractTransaction atx = redisClient.transaction(false)) {

                String redisKey = keyPrefix + key.toString();
                atx.watch(redisKey);

                Response<String> currentValue = atx.get(redisKey);

                if (currentValue.get() != null) {
                    return false; // Key already exists, do not overwrite
                }

                String newValueJson = objectMapper.writeValueAsString(value);

                atx.multi();
                atx.set(redisKey, newValueJson, SetParams.setParams().ex(DEFAULT_EXPIRATION_SECONDS));
                List<Object> results = atx.exec();

                if (results == null) {
                    // Transaction failed due to a watch condition, retry
                    continue;
                }

                return true;
            } catch (Exception e) {
                // Handle the exception (e.g., log it)
                System.err.println("Error during putIfAbsent: " + e.getMessage());
                // Optionally, you can add a small delay before retrying
                try {
                    Thread.sleep(100); // Sleep for 100 milliseconds before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Restore interrupted status
                    throw new RuntimeException("Thread interrupted during retry", ie);
                }
            }
        }

        throw new RuntimeException("Failed to putIfAbsent after " + MAX_RETRIES + " retries");
    }

    @Override
    public void remove(K key) {
        try {
            String redisKey = keyPrefix + key.toString();
            redisClient.del(redisKey);
        } catch (Exception e) {
            // Handle the exception (e.g., log it)
            System.err.println("Error during remove: " + e.getMessage());
        }
    }

    @Override
    public int size() {
        try {
            String pattern = keyPrefix + "*";
            return redisClient.keys(pattern).size();
        } catch (Exception e) {
            // Handle the exception (e.g., log it)
            System.err.println("Error during size retrieval: " + e.getMessage());
        }
        return 0;
    }

}
