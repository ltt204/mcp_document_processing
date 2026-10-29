package individual.ltt204.config;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import individual.ltt204.entities.Job;
import individual.ltt204.services.storage.ActiveFutureStorageService;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.services.storage.JobStorageService;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;
import tools.jackson.databind.ObjectMapper;

public class Config {
    public static ExecutorService getJobExecutor() {
        return new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(2));
    }

    public static ScheduledExecutorService getJobScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    public static ServerCapabilities getServerCapabilities() {
        return ServerCapabilities.builder()
                .tools(true) // Enable tool support with list changes
                .build();
    }

    public static IStorageService<Job> getJobStorageService() {
        return new JobStorageService();
    }

    public static IStorageService<Future<?>> getFutureActiveStorageService() {
        return new ActiveFutureStorageService();
    }

    public static StdioServerTransportProvider getStdioServerTransportProvider() {
        return new StdioServerTransportProvider(McpJsonDefaults.getMapper());
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
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
