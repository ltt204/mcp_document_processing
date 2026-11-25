package individual.ltt204.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.github.cdimascio.dotenv.Dotenv;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
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

    public static StdioServerTransportProvider getStdioServerTransportProvider() {
        return new StdioServerTransportProvider(McpJsonDefaults.getMapper());
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    public static Dotenv getDotenv() {
        return Dotenv.configure().ignoreIfMissing().load();
    }
}
