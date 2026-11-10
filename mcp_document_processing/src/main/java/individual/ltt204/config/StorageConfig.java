package individual.ltt204.config;

import java.time.Duration;
import java.util.concurrent.Future;

import individual.ltt204.entities.Job;
import individual.ltt204.services.storage.ActiveFutureStorageService;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.services.storage.JobStorageService;
import individual.ltt204.services.storage.methods.KeyValueStore;
import individual.ltt204.services.storage.methods.RedisKeyValueStore;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

public class StorageConfig {
    public static IStorageService<Job> getJobStorageService() {
        // KeyValueStore<String, Job> jobStorage = new
        // ConcurrentHashMapKeyValueStore<>();
        String redisHost = Config.getDotenv().get("REDIS_HOST");
        int redisPort = Integer.parseInt(Config.getDotenv().get("REDIS_PORT"));
        String redisPassword = Config.getDotenv().get("REDIS_PASSWORD");

        KeyValueStore<String, Job> jobStorage = new RedisKeyValueStore<>(
                getRedisClient(redisHost, redisPort, redisPassword), "jobs:", Job.class, 3, 3600);

        return new JobStorageService(jobStorage);
    }

    public static IStorageService<Future<?>> getFutureActiveStorageService() {
        return new ActiveFutureStorageService();
    }

    public static ConnectionPoolConfig getRedisPoolConfig() {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();

        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);

        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofSeconds(1));

        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));

        return poolConfig;
    }

    public static RedisClient getRedisClient(String redisHost, int redisPort, String redisPassword) {
        RedisClient redisClient = RedisClient.builder()
                .hostAndPort(redisHost, redisPort)
                .clientConfig(DefaultJedisClientConfig.builder()
                        .password(redisPassword)
                        .build())
                .poolConfig(getRedisPoolConfig())
                .build();

        return redisClient;
    }
}
