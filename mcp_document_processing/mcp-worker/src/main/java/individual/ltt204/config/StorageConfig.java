package individual.ltt204.config;

import java.time.Duration;

import individual.ltt204.entities.Job;
import individual.ltt204.services.storage.IStorageGetOnlyService;
import individual.ltt204.services.storage.JobStorageService;
import individual.ltt204.services.storage.methods.KeyValueStore;
import individual.ltt204.services.storage.methods.RedisKeyValueStore;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

public class StorageConfig {
    public static IStorageGetOnlyService<Job> getJobStorageService() {
        KeyValueStore<String, Job> jobStorage = new RedisKeyValueStore<>(
                getRedisClient(),
                "jobs:", Job.class, 3, 3600);

        return new JobStorageService(jobStorage);
    }

    public static ConnectionPoolConfig getRedisPoolConfig() {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();

        // TODO: Should be in .env file, but for now, we hardcode it here.
        poolConfig.setMaxTotal(8);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(0);

        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofSeconds(1));

        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(1));

        return poolConfig;
    }

    public static RedisClient getRedisClient() {
        String redisHost = Config.getDotenv().get("REDIS_HOST");
        int redisPort = Integer.parseInt(Config.getDotenv().get("REDIS_PORT"));
        String redisPassword = Config.getDotenv().get("REDIS_PASSWORD");

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
