package individual.ltt204.config;

import java.util.concurrent.Future;

import individual.ltt204.entities.CallEntry;
import individual.ltt204.entities.Job;
import individual.ltt204.services.storage.ActiveFutureStorageService;
import individual.ltt204.services.storage.CallsEntryStorageService;
import individual.ltt204.services.storage.IStorageService;
import individual.ltt204.services.storage.JobStorageService;
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.ObjectMapper;

public class Config {
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

}
